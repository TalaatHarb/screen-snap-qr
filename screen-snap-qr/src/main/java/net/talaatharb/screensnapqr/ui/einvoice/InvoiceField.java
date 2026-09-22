package net.talaatharb.screensnapqr.ui.einvoice;

import java.util.Map;

import lombok.Getter;

/**
 * Describes the human-readable meaning of a single UBL element path within a
 * given element context of the EU e-invoicing (EN 16931 / Peppol BIS Billing
 * 3.0) schema.
 */
@Getter
public class InvoiceField {

    private final String code;
    private final String label;
    private final Map<String, String> valueMap;

    public InvoiceField(String code, String label) {
        this(code, label, Map.of());
    }

    public InvoiceField(String code, String label, Map<String, String> valueMap) {
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
