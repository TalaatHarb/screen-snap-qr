package net.talaatharb.screensnapqr.ui.dscsa;

import java.util.LinkedHashMap;
import java.util.Map;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Converts a parsed {@link Gs1Document} into shareable, human-readable text
 * and JSON representations (used both for clipboard copy and file export).
 */
public final class Gs1Exporter {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private Gs1Exporter() {
    }

    /** Renders the document as a labeled, plain-text report. */
    public static String toPlainText(Gs1Document document) {
        final StringBuilder text = new StringBuilder();
        text.append("US DSCSA Package Identifier (GS1 DataMatrix)").append(System.lineSeparator())
                .append(System.lineSeparator());

        for (Map.Entry<String, String> element : document.getElements().entrySet()) {
            text.append(Gs1FieldDictionary.getLabel(element.getKey())).append(" (AI ").append(element.getKey())
                    .append("): ").append(element.getValue()).append(System.lineSeparator());
        }

        return text.toString().stripTrailing() + System.lineSeparator();
    }

    /** Renders the document as pretty-printed JSON, using resolved human-readable labels as keys. */
    public static String toJson(Gs1Document document) throws JsonProcessingException {
        final Map<String, Object> root = new LinkedHashMap<>();
        root.put("type", "US DSCSA Package Identifier");

        final Map<String, String> elements = new LinkedHashMap<>();
        for (Map.Entry<String, String> element : document.getElements().entrySet()) {
            elements.put("AI " + element.getKey() + " - " + Gs1FieldDictionary.getLabel(element.getKey()), element.getValue());
        }
        root.put("elements", elements);

        return OBJECT_MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(root);
    }
}
