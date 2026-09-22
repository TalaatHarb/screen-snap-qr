package net.talaatharb.screensnapqr.ui.einvoice;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

/**
 * Detects and parses XML content that follows the EU e-invoicing standard
 * (EN 16931), as encoded by UBL 2.1 {@code Invoice}/{@code CreditNote}
 * documents and profiled by Peppol BIS Billing 3.0, turning the element tree
 * into a structured, human-readable {@link InvoiceDocument}.
 */
public final class InvoiceParser {

    /** UBL 2.1 namespace declared by the {@code Invoice} root element. */
    public static final String INVOICE_NAMESPACE = "urn:oasis:names:specification:ubl:schema:xsd:Invoice-2";
    /** UBL 2.1 namespace declared by the {@code CreditNote} root element. */
    public static final String CREDIT_NOTE_NAMESPACE = "urn:oasis:names:specification:ubl:schema:xsd:CreditNote-2";

    private static final Set<String> ROOT_ELEMENT_NAMES = Set.of("Invoice", "CreditNote");
    private static final Set<String> ROOT_NAMESPACES = Set.of(INVOICE_NAMESPACE, CREDIT_NOTE_NAMESPACE);
    private static final Set<String> ROOT_SIGNATURE_ELEMENTS = Set.of("ID", "IssueDate", "DocumentCurrencyCode",
            "AccountingSupplierParty", "AccountingCustomerParty", "LegalMonetaryTotal");
    private static final int MIN_SIGNATURE_MATCHES = 4;

    private InvoiceParser() {
    }

    /**
     * Attempts to parse the given XML text as a UBL EU e-invoicing document.
     *
     * @return the parsed document, or {@link Optional#empty()} if the content is
     *         not valid XML or does not match the expected schema shape.
     */
    public static Optional<InvoiceDocument> tryParse(String xmlText) {
        if (xmlText == null || xmlText.isBlank() || !xmlText.stripLeading().startsWith("<")) {
            return Optional.empty();
        }

        try {
            final var document = parseDocument(xmlText);
            final Element root = document.getDocumentElement();
            if (root == null || !matchesSchema(root)) {
                return Optional.empty();
            }
            return Optional.of(toInvoiceDocument(root));
        } catch (ParserConfigurationException | SAXException | IOException | RuntimeException ex) {
            return Optional.empty();
        }
    }

    /** Returns {@code true} if the root element looks like a UBL {@code Invoice}/{@code CreditNote} document. */
    static boolean matchesSchema(Element root) {
        final String localName = localName(root);
        if (!ROOT_ELEMENT_NAMES.contains(localName)) {
            return false;
        }

        final String namespaceUri = root.getNamespaceURI();
        if (namespaceUri != null && !ROOT_NAMESPACES.contains(namespaceUri)) {
            return false;
        }

        int signatureMatches = 0;
        final NodeList children = root.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            final Node node = children.item(i);
            if (node.getNodeType() != Node.ELEMENT_NODE) {
                continue;
            }
            final String childName = localName((Element) node);
            if (ROOT_SIGNATURE_ELEMENTS.contains(childName) || childName.endsWith("TypeCode")) {
                signatureMatches++;
            }
        }
        return signatureMatches >= MIN_SIGNATURE_MATCHES;
    }

    private static InvoiceDocument toInvoiceDocument(Element root) {
        final String rootElementName = localName(root);

        Map<String, String> supplierParty = null;
        Map<String, String> customerParty = null;
        Map<String, String> paymentMeans = null;
        Map<String, String> taxTotal = null;
        Map<String, String> monetaryTotal = null;
        final List<Map<String, String>> lines = new ArrayList<>();

        final NodeList children = root.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            final Node node = children.item(i);
            if (node.getNodeType() != Node.ELEMENT_NODE) {
                continue;
            }
            final Element element = (Element) node;
            final String name = localName(element);
            switch (name) {
                case "AccountingSupplierParty" -> supplierParty = flattenParty(element);
                case "AccountingCustomerParty" -> customerParty = flattenParty(element);
                case "PaymentMeans" -> {
                    if (paymentMeans == null) {
                        paymentMeans = flatten(element);
                    }
                }
                case "TaxTotal" -> {
                    if (taxTotal == null) {
                        taxTotal = flatten(element);
                    }
                }
                case "LegalMonetaryTotal" -> monetaryTotal = flatten(element);
                case "InvoiceLine", "CreditNoteLine" -> lines.add(flatten(element));
                default -> {
                    // Unrecognized or non-scalar child element; ignored for visualization purposes.
                }
            }
        }

        return InvoiceDocument.builder()
                .rootElementName(rootElementName)
                .headerFields(directLeafChildren(root))
                .supplierParty(supplierParty)
                .customerParty(customerParty)
                .paymentMeans(paymentMeans)
                .taxTotal(taxTotal)
                .monetaryTotal(monetaryTotal)
                .lines(lines)
                .build();
    }

    /** Party wrappers ({@code AccountingSupplierParty}/{@code AccountingCustomerParty}) nest the real data under {@code Party}. */
    private static Map<String, String> flattenParty(Element wrapper) {
        final NodeList children = wrapper.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            final Node node = children.item(i);
            if (node.getNodeType() == Node.ELEMENT_NODE && "Party".equals(localName((Element) node))) {
                return flatten((Element) node);
            }
        }
        return flatten(wrapper);
    }

    /** Captures only the direct child elements that have no element children of their own (simple/scalar fields). */
    private static Map<String, String> directLeafChildren(Element element) {
        final Map<String, String> result = new LinkedHashMap<>();
        final NodeList children = element.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            final Node node = children.item(i);
            if (node.getNodeType() != Node.ELEMENT_NODE) {
                continue;
            }
            final Element child = (Element) node;
            if (!hasElementChildren(child)) {
                putIfNotBlank(result, localName(child), decoratedText(child));
            }
        }
        return result;
    }

    /**
     * Recursively flattens the leaf (scalar) descendants of an element into a map keyed by their
     * "/"-separated path relative to {@code element}, so codes that are reused with a different
     * meaning at a different nesting level (for example {@code CompanyID}) remain distinguishable.
     */
    private static Map<String, String> flatten(Element element) {
        final Map<String, String> result = new LinkedHashMap<>();
        flattenInto(element, "", result);
        return result;
    }

    private static void flattenInto(Element element, String pathPrefix, Map<String, String> result) {
        final NodeList children = element.getChildNodes();
        boolean hasElementChild = false;
        for (int i = 0; i < children.getLength(); i++) {
            final Node node = children.item(i);
            if (node.getNodeType() == Node.ELEMENT_NODE) {
                hasElementChild = true;
                final Element child = (Element) node;
                final String path = pathPrefix.isEmpty() ? localName(child) : pathPrefix + "/" + localName(child);
                flattenInto(child, path, result);
            }
        }
        if (!hasElementChild && !pathPrefix.isEmpty()) {
            putIfNotBlank(result, pathPrefix, decoratedText(element));
        }
    }

    private static void putIfNotBlank(Map<String, String> result, String key, String value) {
        if (value != null && !value.isBlank() && !result.containsKey(key)) {
            result.put(key, value);
        }
    }

    /** Appends the {@code currencyID}/{@code unitCode} attribute (common on UBL amount/quantity elements) for readability. */
    private static String decoratedText(Element element) {
        final String text = element.getTextContent() != null ? element.getTextContent().trim() : "";
        if (text.isEmpty()) {
            return text;
        }
        final String currency = element.getAttribute("currencyID");
        if (!currency.isBlank()) {
            return text + " " + currency;
        }
        final String unit = element.getAttribute("unitCode");
        if (!unit.isBlank()) {
            return text + " " + unit;
        }
        return text;
    }

    private static boolean hasElementChildren(Element element) {
        final NodeList children = element.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            if (children.item(i).getNodeType() == Node.ELEMENT_NODE) {
                return true;
            }
        }
        return false;
    }

    private static String localName(Element element) {
        return element.getLocalName() != null ? element.getLocalName() : element.getTagName();
    }

    private static org.w3c.dom.Document parseDocument(String xmlText)
            throws ParserConfigurationException, SAXException, IOException {
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
