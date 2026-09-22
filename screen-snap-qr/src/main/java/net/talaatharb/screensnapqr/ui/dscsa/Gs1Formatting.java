package net.talaatharb.screensnapqr.ui.dscsa;

/**
 * Shared formatting helpers used by both {@link Gs1Parser} (decoding) and
 * {@link Gs1View}/{@link Gs1Exporter} (rendering/export).
 */
final class Gs1Formatting {

    private Gs1Formatting() {
    }

    /**
     * Formats a GS1 6-digit {@code YYMMDD} date value (used by AIs 11/17) as an
     * ISO-8601 date string, assuming years 00-49 are 20xx and 50-99 are 19xx per
     * the GS1 General Specifications.
     */
    static String formatGs1Date(String yymmdd) {
        if (yymmdd == null || yymmdd.length() != 6 || !yymmdd.chars().allMatch(Character::isDigit)) {
            return yymmdd;
        }
        final int yy = Integer.parseInt(yymmdd.substring(0, 2));
        final String month = yymmdd.substring(2, 4);
        final String day = yymmdd.substring(4, 6);
        final int year = yy <= 49 ? 2000 + yy : 1900 + yy;
        return year + "-" + month + "-" + day;
    }
}
