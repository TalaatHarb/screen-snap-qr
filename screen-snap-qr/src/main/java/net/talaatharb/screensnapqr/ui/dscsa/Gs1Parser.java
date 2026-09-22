package net.talaatharb.screensnapqr.ui.dscsa;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.talaatharb.screensnapqr.ui.dscsa.Gs1FieldDictionary.AiDefinition;

/**
 * Detects and parses GS1 Application Identifier (AI) element strings used by
 * US DSCSA (Drug Supply Chain Security Act) pharmaceutical serialization Data
 * Matrix barcodes, in either of the two common textual representations:
 * <ul>
 * <li>human-readable bracketed form, e.g. {@code (01)00312345678907(17)251231(10)ABC123(21)987654321}</li>
 * <li>raw GS1 element string, where variable-length AIs are terminated by the
 * ASCII Group Separator character ({@code 0x1D}, as commonly emitted in place
 * of the FNC1 barcode symbol by 2D barcode decoders) instead of parentheses</li>
 * </ul>
 */
public final class Gs1Parser {

    private static final char GROUP_SEPARATOR = (char) 0x1D;
    private static final Pattern BRACKETED_AI = Pattern.compile("\\((\\d{2,4})\\)([^()]*)");

    private Gs1Parser() {
    }

    /**
     * Attempts to parse the given text as a GS1 AI element string.
     *
     * @return the parsed document, or {@link Optional#empty()} if the content
     *         does not look like GS1 AI data, or lacks the minimum signature
     *         of AIs expected for a DSCSA package identifier (GTIN plus at
     *         least one of batch/lot, expiration date, or serial number).
     */
    public static Optional<Gs1Document> tryParse(String text) {
        if (text == null || text.isBlank()) {
            return Optional.empty();
        }
        final String trimmed = text.strip();

        Map<String, String> elements = null;
        if (trimmed.indexOf('(') >= 0) {
            elements = parseBracketed(trimmed);
        } else {
            final String stripped = stripLeadingGroupSeparator(trimmed);
            if (stripped.length() >= 2 && Character.isDigit(stripped.charAt(0)) && Character.isDigit(stripped.charAt(1))) {
                elements = parseRaw(stripped);
            }
        }

        if (elements == null || !matchesSignature(elements)) {
            return Optional.empty();
        }
        return Optional.of(Gs1Document.builder().elements(elements).build());
    }

    /** Returns {@code true} if GTIN (01) is present with at least one of batch/lot, expiry, or serial. */
    private static boolean matchesSignature(Map<String, String> elements) {
        return elements.containsKey("01")
                && (elements.containsKey("10") || elements.containsKey("17") || elements.containsKey("21"));
    }

    private static Map<String, String> parseBracketed(String text) {
        final Matcher matcher = BRACKETED_AI.matcher(text);
        final Map<String, String> elements = new LinkedHashMap<>();
        boolean matchedAny = false;
        while (matcher.find()) {
            matchedAny = true;
            final String aiCode = matcher.group(1);
            final String rawValue = matcher.group(2);
            elements.put(aiCode, decorateValue(aiCode, rawValue));
        }
        return matchedAny ? elements : null;
    }

    private static Map<String, String> parseRaw(String text) {
        final Map<String, String> elements = new LinkedHashMap<>();
        int pos = 0;
        final int length = text.length();
        while (pos < length) {
            if (pos + 2 > length) {
                return null;
            }
            final String aiCode = text.substring(pos, pos + 2);
            final AiDefinition definition = Gs1FieldDictionary.get(aiCode);
            if (definition == null) {
                // Unknown AI: without the full GS1 AI table we cannot reliably determine its
                // length, so we can't safely continue parsing the rest of the string.
                return null;
            }
            pos += 2;

            final String rawValue;
            if (definition.fixedLength() != null) {
                final int end = pos + definition.fixedLength();
                if (end > length) {
                    return null;
                }
                rawValue = text.substring(pos, end);
                pos = end;
            } else {
                final int separatorIndex = text.indexOf(GROUP_SEPARATOR, pos);
                if (separatorIndex >= 0) {
                    rawValue = text.substring(pos, separatorIndex);
                    pos = separatorIndex + 1;
                } else {
                    rawValue = text.substring(pos);
                    pos = length;
                }
            }
            elements.put(aiCode, decorateValue(aiCode, rawValue));
        }
        return elements;
    }

    private static String decorateValue(String aiCode, String rawValue) {
        final AiDefinition definition = Gs1FieldDictionary.get(aiCode);
        if (definition != null && definition.isDate()) {
            return Gs1Formatting.formatGs1Date(rawValue);
        }
        return rawValue;
    }

    private static String stripLeadingGroupSeparator(String text) {
        return !text.isEmpty() && text.charAt(0) == GROUP_SEPARATOR ? text.substring(1) : text;
    }
}
