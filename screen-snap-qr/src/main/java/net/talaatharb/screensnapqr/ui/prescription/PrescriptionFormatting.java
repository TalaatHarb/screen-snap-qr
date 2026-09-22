package net.talaatharb.screensnapqr.ui.prescription;

import java.util.Map;

/**
 * Shared formatting helpers used by both {@link PrescriptionView} (JavaFX
 * rendering) and {@link PrescriptionExporter} (plain-text/JSON export) so
 * medication titles and attribute descriptions stay consistent.
 */
final class PrescriptionFormatting {

    private PrescriptionFormatting() {
    }

    static String medicationTitle(int index, Map<String, String> medication, String... titleAttributeCandidates) {
        for (String candidate : titleAttributeCandidates) {
            final String value = medication.get(candidate);
            if (value != null && !value.isBlank()) {
                return "Medication " + index + " - " + value;
            }
        }
        return "Medication " + index;
    }
}
