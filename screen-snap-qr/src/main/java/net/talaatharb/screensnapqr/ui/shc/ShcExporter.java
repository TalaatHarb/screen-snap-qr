package net.talaatharb.screensnapqr.ui.shc;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import net.talaatharb.screensnapqr.ui.shc.ShcDocument.ShcResource;
import net.talaatharb.screensnapqr.ui.shc.ShcFieldDictionary.Context;

/**
 * Converts a parsed {@link ShcDocument} into shareable, human-readable text
 * and JSON representations (used both for clipboard copy and file export).
 */
public final class ShcExporter {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private ShcExporter() {
    }

    /** Renders the document as a labeled, plain-text report. */
    public static String toPlainText(ShcDocument document) {
        final StringBuilder text = new StringBuilder();
        text.append("SMART Health Card").append(System.lineSeparator()).append(System.lineSeparator());

        text.append("== Header ==").append(System.lineSeparator());
        for (Map.Entry<String, String> attribute : document.getHeaderAttributes().entrySet()) {
            text.append(ShcFieldDictionary.getLabel(Context.HEADER, attribute.getKey())).append(": ")
                    .append(attribute.getValue()).append(System.lineSeparator());
        }
        text.append(System.lineSeparator());

        int index = 1;
        for (ShcResource resource : document.getResources()) {
            final Context context = contextFor(resource.getResourceType());
            text.append("== ").append(ShcFormatting.resourceTitle(index, resource.getResourceType(), null))
                    .append(" ==").append(System.lineSeparator());
            for (Map.Entry<String, String> attribute : resource.getAttributes().entrySet()) {
                text.append(ShcFieldDictionary.getLabel(context, attribute.getKey())).append(": ")
                        .append(attribute.getValue()).append(System.lineSeparator());
            }
            text.append(System.lineSeparator());
            index++;
        }

        return text.toString().stripTrailing() + System.lineSeparator();
    }

    /** Renders the document as pretty-printed JSON, using resolved human-readable labels as keys. */
    public static String toJson(ShcDocument document) throws JsonProcessingException {
        final Map<String, Object> root = new LinkedHashMap<>();
        root.put("type", "SMART Health Card");
        root.put("header", describeAttributes(Context.HEADER, document.getHeaderAttributes()));

        final List<Map<String, Object>> resources = document.getResources().stream().map(resource -> {
            final Map<String, Object> resourceMap = new LinkedHashMap<>();
            resourceMap.put("resourceType", resource.getResourceType());
            resourceMap.put("fields", describeAttributes(contextFor(resource.getResourceType()), resource.getAttributes()));
            return resourceMap;
        }).toList();
        root.put("resources", resources);

        return OBJECT_MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(root);
    }

    private static Map<String, String> describeAttributes(Context context, Map<String, String> attributes) {
        final Map<String, String> described = new LinkedHashMap<>();
        for (Map.Entry<String, String> attribute : attributes.entrySet()) {
            described.put(ShcFieldDictionary.getLabel(context, attribute.getKey()), attribute.getValue());
        }
        return described;
    }

    private static Context contextFor(String resourceType) {
        if (resourceType == null) {
            return Context.GENERIC_RESOURCE;
        }
        return switch (resourceType) {
            case "Patient" -> Context.PATIENT;
            case "Immunization" -> Context.IMMUNIZATION;
            case "Observation" -> Context.OBSERVATION;
            case "Condition" -> Context.CONDITION;
            default -> Context.GENERIC_RESOURCE;
        };
    }
}
