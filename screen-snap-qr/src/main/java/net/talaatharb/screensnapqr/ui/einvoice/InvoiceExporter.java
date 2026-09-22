package net.talaatharb.screensnapqr.ui.einvoice;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import net.talaatharb.screensnapqr.ui.einvoice.InvoiceFieldDictionary.Context;

/**
 * Converts a parsed {@link InvoiceDocument} into shareable, human-readable
 * text and JSON representations (used both for clipboard copy and file
 * export).
 */
public final class InvoiceExporter {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private InvoiceExporter() {
    }

    /** Renders the document as a labeled, plain-text report. */
    public static String toPlainText(InvoiceDocument document) {
        final StringBuilder text = new StringBuilder();
        text.append(document.isCreditNote() ? "UBL Credit Note (<CreditNote>)" : "UBL Invoice (<Invoice>)")
                .append(System.lineSeparator()).append(System.lineSeparator());

        appendSection(text, "Header", Context.HEADER, document.getHeaderFields());
        if (document.getSupplierParty() != null) {
            appendSection(text, "Seller (Supplier)", Context.SUPPLIER_PARTY, document.getSupplierParty());
        }
        if (document.getCustomerParty() != null) {
            appendSection(text, "Buyer (Customer)", Context.CUSTOMER_PARTY, document.getCustomerParty());
        }
        if (document.getPaymentMeans() != null) {
            appendSection(text, "Payment Means", Context.PAYMENT_MEANS, document.getPaymentMeans());
        }
        if (document.getTaxTotal() != null) {
            appendSection(text, "Tax Total", Context.TAX_TOTAL, document.getTaxTotal());
        }
        if (document.getMonetaryTotal() != null) {
            appendSection(text, "Monetary Totals", Context.MONETARY_TOTAL, document.getMonetaryTotal());
        }
        appendLineSection(text, document.isCreditNote() ? "Credit Note Lines" : "Invoice Lines", document.getLines());

        return text.toString().stripTrailing() + System.lineSeparator();
    }

    /** Renders the document as pretty-printed JSON, using resolved human-readable labels as keys. */
    public static String toJson(InvoiceDocument document) throws JsonProcessingException {
        final Map<String, Object> root = new LinkedHashMap<>();
        root.put("type", document.isCreditNote() ? "Credit Note" : "Invoice");
        root.put("header", describeAttributes(Context.HEADER, document.getHeaderFields()));
        if (document.getSupplierParty() != null) {
            root.put("seller", describeAttributes(Context.SUPPLIER_PARTY, document.getSupplierParty()));
        }
        if (document.getCustomerParty() != null) {
            root.put("buyer", describeAttributes(Context.CUSTOMER_PARTY, document.getCustomerParty()));
        }
        if (document.getPaymentMeans() != null) {
            root.put("paymentMeans", describeAttributes(Context.PAYMENT_MEANS, document.getPaymentMeans()));
        }
        if (document.getTaxTotal() != null) {
            root.put("taxTotal", describeAttributes(Context.TAX_TOTAL, document.getTaxTotal()));
        }
        if (document.getMonetaryTotal() != null) {
            root.put("monetaryTotal", describeAttributes(Context.MONETARY_TOTAL, document.getMonetaryTotal()));
        }
        root.put("lines",
                document.getLines().stream().map(line -> describeAttributes(Context.INVOICE_LINE, line)).toList());

        return OBJECT_MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(root);
    }

    private static void appendSection(StringBuilder text, String title, Context context, Map<String, String> attributes) {
        text.append("== ").append(title).append(" ==").append(System.lineSeparator());
        for (Map.Entry<String, String> attribute : attributes.entrySet()) {
            text.append(InvoiceFieldDictionary.getLabel(context, attribute.getKey())).append(": ")
                    .append(InvoiceFieldDictionary.describeValue(context, attribute.getKey(), attribute.getValue()))
                    .append(System.lineSeparator());
        }
        text.append(System.lineSeparator());
    }

    private static void appendLineSection(StringBuilder text, String title, List<Map<String, String>> lines) {
        if (lines.isEmpty()) {
            return;
        }
        text.append("== ").append(title).append(" (").append(lines.size()).append(") ==").append(System.lineSeparator());
        int index = 1;
        for (Map<String, String> line : lines) {
            text.append("-- ").append(InvoiceFormatting.lineTitle(index, line, "Item/Name")).append(" --")
                    .append(System.lineSeparator());
            for (Map.Entry<String, String> attribute : line.entrySet()) {
                text.append("  ").append(InvoiceFieldDictionary.getLabel(Context.INVOICE_LINE, attribute.getKey())).append(": ")
                        .append(InvoiceFieldDictionary.describeValue(Context.INVOICE_LINE, attribute.getKey(), attribute.getValue()))
                        .append(System.lineSeparator());
            }
            index++;
        }
        text.append(System.lineSeparator());
    }

    private static Map<String, String> describeAttributes(Context context, Map<String, String> attributes) {
        final Map<String, String> described = new LinkedHashMap<>();
        for (Map.Entry<String, String> attribute : attributes.entrySet()) {
            described.put(InvoiceFieldDictionary.getLabel(context, attribute.getKey()),
                    InvoiceFieldDictionary.describeValue(context, attribute.getKey(), attribute.getValue()));
        }
        return described;
    }
}
