package net.talaatharb.screensnapqr.ui.shc;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * Extracts a human-readable summary ({@code Map<String, String>} of field
 * code to raw/decoded value) from a single FHIR resource {@link JsonNode}
 * carried inside a SMART Health Card's {@code fhirBundle}. Only the resource
 * types most commonly found on health cards get a tailored extraction;
 * anything else falls back to a generic flattening of top-level scalar
 * fields.
 */
final class FhirResourceSummarizer {

    private static final String CVX_SYSTEM = "http://hl7.org/fhir/sid/cvx";

    private FhirResourceSummarizer() {
    }

    static Map<String, String> summarize(JsonNode resource) {
        final String resourceType = textValue(resource.get("resourceType"));
        return switch (resourceType != null ? resourceType : "") {
            case "Patient" -> patient(resource);
            case "Immunization" -> immunization(resource);
            case "Observation" -> observation(resource);
            case "Condition" -> condition(resource);
            default -> generic(resource);
        };
    }

    private static Map<String, String> patient(JsonNode resource) {
        final Map<String, String> fields = new LinkedHashMap<>();
        putIfPresent(fields, "name", humanName(resource.get("name")));
        putIfPresent(fields, "birthDate", textValue(resource.get("birthDate")));
        putIfPresent(fields, "gender", textValue(resource.get("gender")));
        return fields;
    }

    private static Map<String, String> immunization(JsonNode resource) {
        final Map<String, String> fields = new LinkedHashMap<>();
        putIfPresent(fields, "status", textValue(resource.get("status")));
        putIfPresent(fields, "vaccineCode", codeableConcept(resource.get("vaccineCode"), CVX_SYSTEM));
        putIfPresent(fields, "occurrenceDateTime", textValue(resource.get("occurrenceDateTime")));
        putIfPresent(fields, "lotNumber", textValue(resource.get("lotNumber")));
        putIfPresent(fields, "performer", performer(resource.get("performer")));
        final JsonNode protocolApplied = firstOf(resource.get("protocolApplied"));
        if (protocolApplied != null) {
            putIfPresent(fields, "doseNumber", textValue(protocolApplied.get("doseNumberPositiveInt")));
            putIfPresent(fields, "seriesDoses", textValue(protocolApplied.get("seriesDosesPositiveInt")));
        }
        return fields;
    }

    private static Map<String, String> observation(JsonNode resource) {
        final Map<String, String> fields = new LinkedHashMap<>();
        putIfPresent(fields, "status", textValue(resource.get("status")));
        putIfPresent(fields, "code", codeableConcept(resource.get("code"), null));
        putIfPresent(fields, "value", observationValue(resource));
        putIfPresent(fields, "effectiveDateTime", textValue(resource.get("effectiveDateTime")));
        return fields;
    }

    private static Map<String, String> condition(JsonNode resource) {
        final Map<String, String> fields = new LinkedHashMap<>();
        putIfPresent(fields, "code", codeableConcept(resource.get("code"), null));
        putIfPresent(fields, "clinicalStatus", codeableConcept(resource.get("clinicalStatus"), null));
        putIfPresent(fields, "onsetDateTime", textValue(resource.get("onsetDateTime")));
        return fields;
    }

    /** Generic fallback: flattens top-level scalar and array-of-scalar fields. */
    private static Map<String, String> generic(JsonNode resource) {
        final Map<String, String> fields = new LinkedHashMap<>();
        final Iterator<Map.Entry<String, JsonNode>> propertyIterator = resource.fields();
        while (propertyIterator.hasNext()) {
            final Map.Entry<String, JsonNode> entry = propertyIterator.next();
            if ("resourceType".equals(entry.getKey())) {
                continue;
            }
            final JsonNode value = entry.getValue();
            if (value.isValueNode()) {
                fields.put(entry.getKey(), value.asText());
            } else if (value.isArray() && allScalars(value)) {
                final List<String> values = new ArrayList<>();
                value.forEach(item -> values.add(item.asText()));
                fields.put(entry.getKey(), String.join(", ", values));
            }
        }
        return fields;
    }

    private static boolean allScalars(JsonNode array) {
        for (JsonNode item : array) {
            if (!item.isValueNode()) {
                return false;
            }
        }
        return true;
    }

    private static String humanName(JsonNode nameArray) {
        final JsonNode name = firstOf(nameArray);
        if (name == null) {
            return null;
        }
        if (name.has("text")) {
            return textValue(name.get("text"));
        }
        final List<String> parts = new ArrayList<>();
        final JsonNode given = name.get("given");
        if (given != null && given.isArray()) {
            given.forEach(part -> parts.add(part.asText()));
        }
        final String family = textValue(name.get("family"));
        if (family != null) {
            parts.add(family);
        }
        return parts.isEmpty() ? null : String.join(" ", parts);
    }

    private static String performer(JsonNode performerArray) {
        final JsonNode performer = firstOf(performerArray);
        if (performer == null) {
            return null;
        }
        final JsonNode actor = performer.get("actor");
        if (actor != null && actor.has("display")) {
            return textValue(actor.get("display"));
        }
        return null;
    }

    private static String codeableConcept(JsonNode codeableConcept, String preferredCodeSystem) {
        if (codeableConcept == null || codeableConcept.isMissingNode() || codeableConcept.isNull()) {
            return null;
        }
        if (codeableConcept.has("text")) {
            return textValue(codeableConcept.get("text"));
        }
        final JsonNode codingArray = codeableConcept.get("coding");
        if (codingArray == null || !codingArray.isArray() || codingArray.isEmpty()) {
            return null;
        }
        JsonNode coding = firstOf(codingArray);
        if (preferredCodeSystem != null) {
            for (JsonNode candidate : codingArray) {
                if (preferredCodeSystem.equals(textValue(candidate.get("system")))) {
                    coding = candidate;
                    break;
                }
            }
        }
        if (coding.has("display")) {
            return textValue(coding.get("display"));
        }
        final String code = textValue(coding.get("code"));
        final String system = textValue(coding.get("system"));
        if (CVX_SYSTEM.equals(system)) {
            return ShcFieldDictionary.describeCvxCode(code);
        }
        return code;
    }

    private static String observationValue(JsonNode resource) {
        if (resource.has("valueString")) {
            return textValue(resource.get("valueString"));
        }
        if (resource.has("valueQuantity")) {
            final JsonNode quantity = resource.get("valueQuantity");
            final String value = textValue(quantity.get("value"));
            final String unit = quantity.has("unit") ? textValue(quantity.get("unit")) : textValue(quantity.get("code"));
            return unit != null ? value + " " + unit : value;
        }
        if (resource.has("valueCodeableConcept")) {
            return codeableConcept(resource.get("valueCodeableConcept"), null);
        }
        return null;
    }

    private static JsonNode firstOf(JsonNode arrayNode) {
        if (arrayNode == null || !arrayNode.isArray() || arrayNode.isEmpty()) {
            return null;
        }
        return arrayNode.get(0);
    }

    private static String textValue(JsonNode node) {
        return node == null || node.isMissingNode() || node.isNull() ? null : node.asText();
    }

    private static void putIfPresent(Map<String, String> fields, String key, String value) {
        if (value != null && !value.isBlank()) {
            fields.put(key, value);
        }
    }
}
