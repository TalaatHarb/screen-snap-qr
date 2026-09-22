package net.talaatharb.screensnapqr.ui.einvoice;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

class InvoiceExporterTest {

    private static InvoiceDocument sampleDocument() {
        return InvoiceDocument.builder()
                .rootElementName("Invoice")
                .headerFields(Map.of("ID", "INV-2024-001", "InvoiceTypeCode", "380"))
                .supplierParty(Map.of("PartyName/Name", "Acme Supplies SRL"))
                .customerParty(Map.of("PartyLegalEntity/RegistrationName", "Beta Buyer SA"))
                .monetaryTotal(Map.of("PayableAmount", "119.00 EUR"))
                .lines(List.of(Map.of("Item/Name", "Widget", "LineExtensionAmount", "100.00 EUR")))
                .build();
    }

    @Test
    void testToPlainTextContainsResolvedLabelsAndValues() {
        final String text = InvoiceExporter.toPlainText(sampleDocument());

        assertTrue(text.contains("UBL Invoice (<Invoice>)"));
        assertTrue(text.contains("Invoice Number (BT-1): INV-2024-001"));
        assertTrue(text.contains("Invoice Type Code (BT-3): Commercial invoice"));
        assertTrue(text.contains("Seller Trading Name (BT-28): Acme Supplies SRL"));
        assertTrue(text.contains("Buyer Legal Name (BT-44): Beta Buyer SA"));
        assertTrue(text.contains("Amount Due for Payment (BT-115): 119.00 EUR"));
        assertTrue(text.contains("Invoice Lines (1)"));
        assertTrue(text.contains("Line 1 - Widget"));
    }

    @Test
    void testToJsonContainsResolvedLabelsAndValues() throws Exception {
        final String json = InvoiceExporter.toJson(sampleDocument());

        assertTrue(json.contains("\"type\""));
        assertTrue(json.contains("Invoice Number (BT-1)"));
        assertTrue(json.contains("\"INV-2024-001\""));
        assertTrue(json.contains("\"lines\""));
        assertTrue(json.contains("Widget"));
    }
}
