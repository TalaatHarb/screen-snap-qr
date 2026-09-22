package net.talaatharb.screensnapqr.ui.shc;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

/**
 * Shared formatting helpers used by both {@link ShcView} (JavaFX rendering)
 * and {@link ShcExporter} (plain-text/JSON export).
 */
final class ShcFormatting {

    private static final DateTimeFormatter DATE_TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
            .withZone(ZoneOffset.UTC);

    private ShcFormatting() {
    }

    /** Formats a JWT numeric-date (seconds since epoch, possibly fractional) as a readable UTC timestamp. */
    static String formatEpochSeconds(Number epochSeconds) {
        if (epochSeconds == null) {
            return null;
        }
        final long millis = Math.round(epochSeconds.doubleValue() * 1000);
        return DATE_TIME_FORMAT.format(Instant.ofEpochMilli(millis)) + " UTC";
    }

    static String resourceTitle(int index, String resourceType, String secondaryLabel) {
        final String base = resourceType != null && !resourceType.isBlank() ? resourceType : "Resource";
        if (secondaryLabel != null && !secondaryLabel.isBlank()) {
            return index + ". " + base + " - " + secondaryLabel;
        }
        return index + ". " + base;
    }
}
