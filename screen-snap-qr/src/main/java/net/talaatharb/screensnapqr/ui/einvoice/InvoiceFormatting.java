package net.talaatharb.screensnapqr.ui.einvoice;

import java.util.Map;

/**
 * Shared formatting helpers used by both {@link InvoiceView} (JavaFX
 * rendering) and {@link InvoiceExporter} (plain-text/JSON export) so invoice
 * line titles and attribute descriptions stay consistent.
 */
final class InvoiceFormatting {

    private InvoiceFormatting() {
    }

    static String lineTitle(int index, Map<String, String> line, String... titleFieldCandidates) {
        final String id = line.get("ID");
        for (String candidate : titleFieldCandidates) {
            final String value = line.get(candidate);
            if (value != null && !value.isBlank()) {
                return id != null && !id.isBlank() ? "Line " + id + " - " + value : "Line " + index + " - " + value;
            }
        }
        return id != null && !id.isBlank() ? "Line " + id : "Line " + index;
    }
}
