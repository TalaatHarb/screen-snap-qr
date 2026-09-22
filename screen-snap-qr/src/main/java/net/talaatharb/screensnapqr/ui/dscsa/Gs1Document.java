package net.talaatharb.screensnapqr.ui.dscsa;

import java.util.LinkedHashMap;
import java.util.Map;

import lombok.Builder;
import lombok.Getter;

/**
 * Parsed representation of a GS1 Application Identifier (AI) element string,
 * as used by US DSCSA (Drug Supply Chain Security Act) pharmaceutical
 * serialization Data Matrix barcodes.
 */
@Getter
@Builder
public class Gs1Document {

    /** Raw AI code (e.g. {@code "01"}) to decoded/formatted value, in encounter order. */
    @Builder.Default
    private final Map<String, String> elements = new LinkedHashMap<>();

    public String getGtin() {
        return elements.get("01");
    }

    public String getBatchOrLot() {
        return elements.get("10");
    }

    public String getExpirationDate() {
        return elements.get("17");
    }

    public String getProductionDate() {
        return elements.get("11");
    }

    public String getSerialNumber() {
        return elements.get("21");
    }
}
