package net.talaatharb.screensnapqr.ui.prescription;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

class PrescriptionExporterTest {

    private static PrescriptionDocument sampleDocument() {
        return PrescriptionDocument.builder()
                .rootElementName("P")
                .headerAttributes(Map.of("SC", "AB", "SN", "1234567"))
                .prescriptionDetails(Map.of("FN", "Ion", "LN", "Popescu"))
                .pharmacyDetails(Map.of("CC", "777666"))
                .prescribedMedications(List.of(Map.of("AS", "Paracetamolum", "Q", "20")))
                .dispensedMedications(List.of(Map.of("D", "W12345", "IQ", "20")))
                .build();
    }

    @Test
    void testToPlainTextContainsResolvedLabelsAndValues() {
        final String text = PrescriptionExporter.toPlainText(sampleDocument());

        assertTrue(text.contains("Online Prescription"));
        assertTrue(text.contains("Prescription Series (Seria rețetei): AB"));
        assertTrue(text.contains("Patient First Name (Prenume asigurat): Ion"));
        assertTrue(text.contains("Pharmacy Fiscal Code (Codul fiscal al farmaciei): 777666"));
        assertTrue(text.contains("Prescribed Medications (1)"));
        assertTrue(text.contains("Medication 1 - Paracetamolum"));
        assertTrue(text.contains("Dispensed Medications (1)"));
    }

    @Test
    void testToJsonContainsResolvedLabelsAndValues() throws Exception {
        final String json = PrescriptionExporter.toJson(sampleDocument());

        assertTrue(json.contains("\"type\""));
        assertTrue(json.contains("Prescription Series (Seria rețetei)"));
        assertTrue(json.contains("\"AB\""));
        assertTrue(json.contains("prescribedMedications"));
        assertTrue(json.contains("dispensedMedications"));
    }
}
