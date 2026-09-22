package net.talaatharb.screensnapqr.ui.prescription;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

/**
 * Detects and parses XML content that follows the Romanian e-prescription 2D
 * barcode schema (PEBarcode.xsd, namespace {@code http://www.cnas.ro/pel/1.0}),
 * turning the two-letter attribute codes into a structured, human-readable
 * {@link PrescriptionDocument}.
 */
public final class PrescriptionParser {

    /** Namespace declared by PEBarcode.xsd; not always present on real-world instances. */
    public static final String SCHEMA_NAMESPACE = "http://www.cnas.ro/pel/1.0";

    private static final List<String> ROOT_ELEMENT_NAMES = List.of("P", "O");
    private static final List<String> ROOT_SIGNATURE_ATTRIBUTES = List.of("SC", "SN", "PS", "CC", "CN", "OU");
    private static final int MIN_SIGNATURE_MATCHES = 2;

    private PrescriptionParser() {
    }

    /**
     * Attempts to parse the given XML text as a Romanian e-prescription document.
     *
     * @return the parsed document, or {@link Optional#empty()} if the content is
     *         not valid XML or does not match the expected schema shape.
     */
    public static Optional<PrescriptionDocument> tryParse(String xmlText) {
        if (xmlText == null || xmlText.isBlank() || !xmlText.stripLeading().startsWith("<")) {
            return Optional.empty();
        }

        try {
            final Document document = parseDocument(xmlText);
            final Element root = document.getDocumentElement();
            if (root == null || !matchesSchema(root)) {
                return Optional.empty();
            }
            return Optional.of(toPrescriptionDocument(root));
        } catch (ParserConfigurationException | SAXException | IOException | RuntimeException ex) {
            return Optional.empty();
        }
    }

    /** Returns {@code true} if the root element looks like a PEBarcode.xsd {@code <P>}/{@code <O>} document. */
    static boolean matchesSchema(Element root) {
        final String localName = root.getLocalName() != null ? root.getLocalName() : root.getTagName();
        if (!ROOT_ELEMENT_NAMES.contains(localName)) {
            return false;
        }

        final String namespaceUri = root.getNamespaceURI();
        if (namespaceUri != null && !SCHEMA_NAMESPACE.equals(namespaceUri)) {
            return false;
        }

        int signatureMatches = 0;
        for (String attribute : ROOT_SIGNATURE_ATTRIBUTES) {
            if (root.hasAttribute(attribute)) {
                signatureMatches++;
            }
        }
        return signatureMatches >= MIN_SIGNATURE_MATCHES;
    }

    private static PrescriptionDocument toPrescriptionDocument(Element root) {
        final String rootElementName = root.getLocalName() != null ? root.getLocalName() : root.getTagName();

        Map<String, String> prescriptionDetails = null;
        Map<String, String> pharmacyDetails = null;
        final List<Map<String, String>> prescribedMedications = new ArrayList<>();
        final List<Map<String, String>> dispensedMedications = new ArrayList<>();

        final NodeList children = root.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            final Node node = children.item(i);
            if (node.getNodeType() != Node.ELEMENT_NODE) {
                continue;
            }
            final Element element = (Element) node;
            final String name = element.getLocalName() != null ? element.getLocalName() : element.getTagName();
            switch (name) {
                case "PD" -> prescriptionDetails = attributesOf(element);
                case "FD" -> pharmacyDetails = attributesOf(element);
                case "D" -> prescribedMedications.add(attributesOf(element));
                case "E" -> dispensedMedications.add(attributesOf(element));
                default -> {
                    // Unrecognized child element; ignored for visualization purposes.
                }
            }
        }

        return PrescriptionDocument.builder()
                .rootElementName(rootElementName)
                .headerAttributes(attributesOf(root))
                .prescriptionDetails(prescriptionDetails)
                .pharmacyDetails(pharmacyDetails)
                .prescribedMedications(prescribedMedications)
                .dispensedMedications(dispensedMedications)
                .build();
    }

    private static Map<String, String> attributesOf(Element element) {
        final Map<String, String> attributes = new LinkedHashMap<>();
        final NamedNodeMap attributeMap = element.getAttributes();
        for (int i = 0; i < attributeMap.getLength(); i++) {
            final Attr attr = (Attr) attributeMap.item(i);
            final String attrName = attr.getLocalName() != null ? attr.getLocalName() : attr.getName();
            if (attrName.startsWith("xmlns")) {
                continue;
            }
            attributes.put(attrName, attr.getValue());
        }
        return attributes;
    }

    private static Document parseDocument(String xmlText) throws ParserConfigurationException, SAXException, IOException {
        final var factory = DocumentBuilderFactory.newInstance();
        factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
        factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
        factory.setNamespaceAware(true);
        factory.setXIncludeAware(false);
        factory.setExpandEntityReferences(false);
        final var documentBuilder = factory.newDocumentBuilder();
        return documentBuilder.parse(new InputSource(new StringReader(xmlText)));
    }
}
