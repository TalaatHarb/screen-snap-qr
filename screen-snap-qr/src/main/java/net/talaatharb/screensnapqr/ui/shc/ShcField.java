package net.talaatharb.screensnapqr.ui.shc;

import java.util.Map;

import lombok.Getter;

/**
 * Describes the human-readable meaning of a single field within a given
 * resource-type context of a SMART Health Card (SHC).
 */
@Getter
public class ShcField {

    private final String code;
    private final String label;
    private final Map<String, String> valueMap;

    public ShcField(String code, String label) {
        this(code, label, Map.of());
    }

    public ShcField(String code, String label, Map<String, String> valueMap) {
        this.code = code;
        this.label = label;
        this.valueMap = valueMap == null ? Map.of() : valueMap;
    }

    /**
     * Resolves the human-readable meaning of a raw value, falling back to the
     * raw value itself when no enumeration mapping is defined.
     */
    public String describeValue(String rawValue) {
        if (rawValue == null) {
            return null;
        }
        return valueMap.getOrDefault(rawValue, rawValue);
    }
}
