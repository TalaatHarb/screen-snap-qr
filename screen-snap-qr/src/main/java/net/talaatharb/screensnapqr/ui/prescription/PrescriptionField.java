package net.talaatharb.screensnapqr.ui.prescription;

import java.util.Map;

import lombok.Getter;

/**
 * Describes the human-readable meaning of a single attribute code within a
 * given element context of the Romanian e-prescription schema (PEBarcode.xsd).
 */
@Getter
public class PrescriptionField {

    private final String code;
    private final String label;
    private final Map<String, String> valueMap;

    public PrescriptionField(String code, String label) {
        this(code, label, Map.of());
    }

    public PrescriptionField(String code, String label, Map<String, String> valueMap) {
        this.code = code;
        this.label = label;
        this.valueMap = valueMap == null ? Map.of() : valueMap;
    }

    /**
     * Resolves the human-readable meaning of a raw attribute value, falling back
     * to the raw value itself when no enumeration mapping is defined.
     */
    public String describeValue(String rawValue) {
        if (rawValue == null) {
            return null;
        }
        return valueMap.getOrDefault(rawValue, rawValue);
    }
}
