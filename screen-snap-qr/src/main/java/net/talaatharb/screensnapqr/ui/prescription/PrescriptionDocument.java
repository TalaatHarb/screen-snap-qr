package net.talaatharb.screensnapqr.ui.prescription;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import lombok.Builder;
import lombok.Getter;

/**
 * Parsed representation of an XML document matching the Romanian e-prescription
 * 2D barcode schema (PEBarcode.xsd).
 */
@Getter
@Builder
public class PrescriptionDocument {

    /** Root element name, either {@code P} (online) or {@code O} (offline). */
    private final String rootElementName;

    /** Attributes carried by the root element ({@code SC}, {@code SN}, {@code PS}, ...). */
    @Builder.Default
    private final Map<String, String> headerAttributes = new LinkedHashMap<>();

    /** Attributes of the optional {@code <PD>} (prescription details) element, or {@code null} if absent. */
    private final Map<String, String> prescriptionDetails;

    /** Attributes of the optional {@code <FD>} (pharmacy details) element, or {@code null} if absent. */
    private final Map<String, String> pharmacyDetails;

    /** One entry per {@code <D>} (prescribed medication) element. */
    @Builder.Default
    private final List<Map<String, String>> prescribedMedications = new ArrayList<>();

    /** One entry per {@code <E>} (dispensed medication) element. */
    @Builder.Default
    private final List<Map<String, String>> dispensedMedications = new ArrayList<>();

    public boolean isOnline() {
        return "P".equals(rootElementName);
    }
}
