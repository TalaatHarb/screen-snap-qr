package net.talaatharb.screensnapqr.ui.shc;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

class ShcExporterTest {

    private static ShcDocument sampleDocument() {
        return ShcDocument.builder()
                .headerAttributes(Map.of("iss", "https://example.org/issuer", "fhirVersion", "4.0.1", "types",
                        "health-card, immunization, covid19"))
                .resources(List.of(
                        ShcDocument.ShcResource.builder().resourceType("Patient")
                                .attributes(Map.of("name", "John B. Anyperson", "birthDate", "1951-01-20"))
                                .rawJson("{\"resourceType\":\"Patient\"}").build(),
                        ShcDocument.ShcResource.builder().resourceType("Immunization")
                                .attributes(Map.of("vaccineCode", "COVID-19 (Moderna)", "lotNumber", "0000001"))
                                .rawJson("{\"resourceType\":\"Immunization\"}").build()))
                .build();
    }

    @Test
    void testToPlainTextContainsResolvedLabelsAndValues() {
        final String text = ShcExporter.toPlainText(sampleDocument());

        assertTrue(text.contains("SMART Health Card"));
        assertTrue(text.contains("Issuer: https://example.org/issuer"));
        assertTrue(text.contains("FHIR Version: 4.0.1"));
        assertTrue(text.contains("1. Patient"));
        assertTrue(text.contains("Name: John B. Anyperson"));
        assertTrue(text.contains("2. Immunization"));
        assertTrue(text.contains("Vaccine: COVID-19 (Moderna)"));
    }

    @Test
    void testToJsonContainsResolvedLabelsAndValues() throws Exception {
        final String json = ShcExporter.toJson(sampleDocument());

        assertTrue(json.contains("\"type\""));
        assertTrue(json.contains("\"resources\""));
        assertTrue(json.contains("\"resourceType\" : \"Patient\""));
        assertTrue(json.contains("John B. Anyperson"));
        assertTrue(json.contains("COVID-19 (Moderna)"));
    }
}
