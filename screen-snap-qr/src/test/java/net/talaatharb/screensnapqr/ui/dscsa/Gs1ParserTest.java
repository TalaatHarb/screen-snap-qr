package net.talaatharb.screensnapqr.ui.dscsa;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;

import org.junit.jupiter.api.Test;

class Gs1ParserTest {

    @Test
    void testParsesBracketedFormatIntoStructuredDocument() {
        final String payload = "(01)00312345678907(17)251231(10)ABC123(21)987654321";

        final Optional<Gs1Document> parsed = Gs1Parser.tryParse(payload);
        assertTrue(parsed.isPresent());

        final Gs1Document document = parsed.get();
        assertEquals("00312345678907", document.getGtin());
        assertEquals("2025-12-31", document.getExpirationDate());
        assertEquals("ABC123", document.getBatchOrLot());
        assertEquals("987654321", document.getSerialNumber());
    }

    @Test
    void testParsesRawGroupSeparatedFormatIntoStructuredDocument() {
        final char gs = (char) 0x1D;
        final String payload = "0100312345678907" + "17251231" + "10ABC123" + gs + "21987654321";

        final Optional<Gs1Document> parsed = Gs1Parser.tryParse(payload);
        assertTrue(parsed.isPresent());

        final Gs1Document document = parsed.get();
        assertEquals("00312345678907", document.getGtin());
        assertEquals("2025-12-31", document.getExpirationDate());
        assertEquals("ABC123", document.getBatchOrLot());
        assertEquals("987654321", document.getSerialNumber());
    }

    @Test
    void testFormatsYyPrefixBelow50As20xx() {
        final Optional<Gs1Document> parsed = Gs1Parser.tryParse("(01)00312345678907(11)490101(17)500101");
        assertTrue(parsed.isPresent());
        assertEquals("2049-01-01", parsed.get().getProductionDate());
        assertEquals("1950-01-01", parsed.get().getExpirationDate());
    }

    @Test
    void testReturnsEmptyWhenGtinMissing() {
        assertFalse(Gs1Parser.tryParse("(17)251231(10)ABC123").isPresent());
    }

    @Test
    void testReturnsEmptyWhenOnlyGtinPresent() {
        assertFalse(Gs1Parser.tryParse("(01)00312345678907").isPresent());
    }

    @Test
    void testReturnsEmptyForUnrelatedText() {
        assertFalse(Gs1Parser.tryParse("not a gs1 payload").isPresent());
        assertFalse(Gs1Parser.tryParse(null).isPresent());
        assertFalse(Gs1Parser.tryParse("").isPresent());
    }

    @Test
    void testReturnsEmptyForRawPayloadWithUnknownAi() {
        // "99" is not in our known-AI table, so the raw (non-bracketed) parser can't
        // safely determine where its value ends.
        assertFalse(Gs1Parser.tryParse("0100312345678907991234").isPresent());
    }
}
