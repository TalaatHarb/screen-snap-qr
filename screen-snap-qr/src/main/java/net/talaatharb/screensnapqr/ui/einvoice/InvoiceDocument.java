package net.talaatharb.screensnapqr.ui.einvoice;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import lombok.Builder;
import lombok.Getter;

/**
 * Parsed representation of a UBL 2.1 {@code Invoice}/{@code CreditNote} document
 * following the EU e-invoicing standard (EN 16931 / Peppol BIS Billing 3.0).
 */
@Getter
@Builder
public class InvoiceDocument {

    /** Root element name, either {@code Invoice} or {@code CreditNote}. */
    private final String rootElementName;

    /** Simple, direct root-level fields (ID, IssueDate, DocumentCurrencyCode, ...). */
    @Builder.Default
    private final Map<String, String> headerFields = new LinkedHashMap<>();

    /** Flattened fields of {@code cac:AccountingSupplierParty/cac:Party} (the seller), or {@code null} if absent. */
    private final Map<String, String> supplierParty;

    /** Flattened fields of {@code cac:AccountingCustomerParty/cac:Party} (the buyer), or {@code null} if absent. */
    private final Map<String, String> customerParty;

    /** Flattened fields of the first {@code cac:PaymentMeans} element, or {@code null} if absent. */
    private final Map<String, String> paymentMeans;

    /** Flattened fields of the first {@code cac:TaxTotal} element, or {@code null} if absent. */
    private final Map<String, String> taxTotal;

    /** Flattened fields of {@code cac:LegalMonetaryTotal}, or {@code null} if absent. */
    private final Map<String, String> monetaryTotal;

    /** One entry per {@code cac:InvoiceLine} / {@code cac:CreditNoteLine} element. */
    @Builder.Default
    private final List<Map<String, String>> lines = new ArrayList<>();

    public boolean isCreditNote() {
        return "CreditNote".equals(rootElementName);
    }
}
