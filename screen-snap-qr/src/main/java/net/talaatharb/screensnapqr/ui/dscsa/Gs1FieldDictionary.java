package net.talaatharb.screensnapqr.ui.dscsa;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Static catalog of the GS1 Application Identifiers (AIs) relevant to US
 * DSCSA pharmaceutical serialization: GTIN, batch/lot, production/expiration
 * date, and serial number.
 */
public final class Gs1FieldDictionary {

    /** Describes a single GS1 Application Identifier's shape. */
    public record AiDefinition(String code, String label, Integer fixedLength, boolean isDate) {
    }

    private static final Map<String, AiDefinition> DEFINITIONS = buildDefinitions();

    private Gs1FieldDictionary() {
    }

    public static AiDefinition get(String aiCode) {
        return DEFINITIONS.get(aiCode);
    }

    public static boolean isKnown(String aiCode) {
        return DEFINITIONS.containsKey(aiCode);
    }

    public static String getLabel(String aiCode) {
        final AiDefinition definition = DEFINITIONS.get(aiCode);
        return definition != null ? definition.label() : "AI (" + aiCode + ")";
    }

    private static Map<String, AiDefinition> buildDefinitions() {
        final Map<String, AiDefinition> definitions = new LinkedHashMap<>();
        definitions.put("01", new AiDefinition("01", "GTIN (Global Trade Item Number)", 14, false));
        definitions.put("10", new AiDefinition("10", "Batch/Lot Number", null, false));
        definitions.put("11", new AiDefinition("11", "Production Date", 6, true));
        definitions.put("17", new AiDefinition("17", "Expiration Date", 6, true));
        definitions.put("21", new AiDefinition("21", "Serial Number", null, false));
        return definitions;
    }
}
