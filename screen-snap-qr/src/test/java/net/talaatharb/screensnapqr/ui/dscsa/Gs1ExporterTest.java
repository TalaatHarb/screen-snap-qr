package net.talaatharb.screensnapqr.ui.dscsa;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

class Gs1ExporterTest {

    private static Gs1Document sampleDocument() {
        final Map<String, String> elements = new LinkedHashMap<>();
        elements.put("01", "00312345678907");
        elements.put("17", "2025-12-31");
        elements.put("10", "ABC123");
        elements.put("21", "987654321");
        return Gs1Document.builder().elements(elements).build();
    }

    @Test
    void testToPlainTextContainsResolvedLabelsAndValues() {
        final String text = Gs1Exporter.toPlainText(sampleDocument());

        assertTrue(text.contains("US DSCSA Package Identifier"));
        assertTrue(text.contains("GTIN (Global Trade Item Number) (AI 01): 00312345678907"));
        assertTrue(text.contains("Expiration Date (AI 17): 2025-12-31"));
        assertTrue(text.contains("Batch/Lot Number (AI 10): ABC123"));
        assertTrue(text.contains("Serial Number (AI 21): 987654321"));
    }

    @Test
    void testToJsonContainsResolvedLabelsAndValues() throws Exception {
        final String json = Gs1Exporter.toJson(sampleDocument());

        assertTrue(json.contains("\"type\""));
        assertTrue(json.contains("\"elements\""));
        assertTrue(json.contains("00312345678907"));
        assertTrue(json.contains("GTIN (Global Trade Item Number)"));
    }
}
