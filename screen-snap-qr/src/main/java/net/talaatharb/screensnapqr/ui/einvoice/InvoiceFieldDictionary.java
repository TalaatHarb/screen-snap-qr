package net.talaatharb.screensnapqr.ui.einvoice;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Static catalog of the UBL 2.1 element names/paths used by the EU
 * e-invoicing standard (EN 16931, as profiled by Peppol BIS Billing 3.0),
 * mapped to their human-readable meaning (including the EN 16931 "BT-"
 * business term identifiers where applicable).
 * <p>
 * Element names are only unique within the sub-tree that declares them (for
 * example {@code CompanyID} means "VAT identifier" under
 * {@code PartyTaxScheme} but "legal registration identifier" under
 * {@code PartyLegalEntity}), so lookups are scoped by {@link Context} and use
 * a "/"-separated relative path where needed.
 */
public final class InvoiceFieldDictionary {

    /** Element scopes in which field paths are declared by the schema. */
    public enum Context {
        /** Direct, simple child elements of the {@code Invoice}/{@code CreditNote} root. */
        HEADER,
        /** {@code cac:AccountingSupplierParty/cac:Party} - the seller. */
        SUPPLIER_PARTY,
        /** {@code cac:AccountingCustomerParty/cac:Party} - the buyer. */
        CUSTOMER_PARTY,
        /** {@code cac:PaymentMeans} - payment instructions. */
        PAYMENT_MEANS,
        /** {@code cac:TaxTotal} - VAT breakdown. */
        TAX_TOTAL,
        /** {@code cac:LegalMonetaryTotal} - document totals. */
        MONETARY_TOTAL,
        /** {@code cac:InvoiceLine} / {@code cac:CreditNoteLine} - a single invoice line. */
        INVOICE_LINE
    }

    private static final Map<Context, Map<String, InvoiceField>> DICTIONARY = buildDictionary();

    private InvoiceFieldDictionary() {
    }

    public static InvoiceField getField(Context context, String code) {
        final Map<String, InvoiceField> fields = DICTIONARY.get(context);
        if (fields == null) {
            return null;
        }
        return fields.get(code);
    }

    public static String getLabel(Context context, String code) {
        final InvoiceField field = getField(context, code);
        return field != null ? field.getLabel() : code;
    }

    public static String describeValue(Context context, String code, String rawValue) {
        final InvoiceField field = getField(context, code);
        return field != null ? field.describeValue(rawValue) : rawValue;
    }

    private static Map<Context, Map<String, InvoiceField>> buildDictionary() {
        final Map<Context, Map<String, InvoiceField>> dictionary = new LinkedHashMap<>();
        dictionary.put(Context.HEADER, header());
        dictionary.put(Context.SUPPLIER_PARTY, supplierParty());
        dictionary.put(Context.CUSTOMER_PARTY, customerParty());
        dictionary.put(Context.PAYMENT_MEANS, paymentMeans());
        dictionary.put(Context.TAX_TOTAL, taxTotal());
        dictionary.put(Context.MONETARY_TOTAL, monetaryTotal());
        dictionary.put(Context.INVOICE_LINE, invoiceLine());
        return dictionary;
    }

    private static Map<String, InvoiceField> header() {
        final Map<String, InvoiceField> fields = new LinkedHashMap<>();
        put(fields, "CustomizationID", "Specification Identifier (BT-24)");
        put(fields, "ProfileID", "Business Process Type (BT-23)");
        put(fields, "ID", "Invoice Number (BT-1)");
        put(fields, "IssueDate", "Issue Date (BT-2)");
        put(fields, "DueDate", "Payment Due Date (BT-9)");
        put(fields, "TaxPointDate", "Value Added Tax Point Date (BT-7)");
        put(fields, "InvoiceTypeCode", "Invoice Type Code (BT-3)", invoiceTypeCodeMap());
        put(fields, "CreditNoteTypeCode", "Credit Note Type Code (BT-3)", invoiceTypeCodeMap());
        put(fields, "Note", "Invoice Note (BT-22)");
        put(fields, "TaxPointDate", "Value Added Tax Point Date (BT-7)");
        put(fields, "DocumentCurrencyCode", "Invoice Currency Code (BT-5)");
        put(fields, "TaxCurrencyCode", "VAT Accounting Currency Code (BT-6)");
        put(fields, "AccountingCost", "Buyer Accounting Reference (BT-19)");
        put(fields, "BuyerReference", "Buyer Reference (BT-10)");
        return fields;
    }

    private static Map<String, InvoiceField> supplierParty() {
        final Map<String, InvoiceField> fields = new LinkedHashMap<>();
        put(fields, "EndpointID", "Seller Electronic Address (BT-34)");
        put(fields, "PartyName/Name", "Seller Trading Name (BT-28)");
        put(fields, "PostalAddress/StreetName", "Seller Address Line 1 (BT-35)");
        put(fields, "PostalAddress/AdditionalStreetName", "Seller Address Line 2 (BT-36)");
        put(fields, "PostalAddress/CityName", "Seller City (BT-37)");
        put(fields, "PostalAddress/PostalZone", "Seller Post Code (BT-38)");
        put(fields, "PostalAddress/CountrySubentity", "Seller Region (BT-39)");
        put(fields, "PostalAddress/Country/IdentificationCode", "Seller Country Code (BT-40)");
        put(fields, "PartyTaxScheme/CompanyID", "Seller VAT Identifier (BT-31)");
        put(fields, "PartyLegalEntity/RegistrationName", "Seller Legal Name (BT-27)");
        put(fields, "PartyLegalEntity/CompanyID", "Seller Legal Registration Identifier (BT-30)");
        put(fields, "Contact/Name", "Seller Contact Point (BT-41)");
        put(fields, "Contact/Telephone", "Seller Contact Telephone Number (BT-42)");
        put(fields, "Contact/ElectronicMail", "Seller Contact Email Address (BT-43)");
        return fields;
    }

    private static Map<String, InvoiceField> customerParty() {
        final Map<String, InvoiceField> fields = new LinkedHashMap<>();
        put(fields, "EndpointID", "Buyer Electronic Address (BT-49)");
        put(fields, "PartyName/Name", "Buyer Trading Name");
        put(fields, "PostalAddress/StreetName", "Buyer Address Line 1 (BT-50)");
        put(fields, "PostalAddress/AdditionalStreetName", "Buyer Address Line 2 (BT-51)");
        put(fields, "PostalAddress/CityName", "Buyer City (BT-52)");
        put(fields, "PostalAddress/PostalZone", "Buyer Post Code (BT-53)");
        put(fields, "PostalAddress/CountrySubentity", "Buyer Region");
        put(fields, "PostalAddress/Country/IdentificationCode", "Buyer Country Code (BT-55)");
        put(fields, "PartyTaxScheme/CompanyID", "Buyer VAT Identifier (BT-48)");
        put(fields, "PartyLegalEntity/RegistrationName", "Buyer Legal Name (BT-44)");
        put(fields, "PartyLegalEntity/CompanyID", "Buyer Legal Registration Identifier (BT-47)");
        put(fields, "Contact/Name", "Buyer Contact Point (BT-56)");
        put(fields, "Contact/Telephone", "Buyer Contact Telephone Number (BT-57)");
        put(fields, "Contact/ElectronicMail", "Buyer Contact Email Address (BT-58)");
        return fields;
    }

    private static Map<String, InvoiceField> paymentMeans() {
        final Map<String, InvoiceField> fields = new LinkedHashMap<>();
        put(fields, "PaymentMeansCode", "Payment Means Type Code (BT-81)", Map.ofEntries(
                Map.entry("30", "Credit transfer"),
                Map.entry("42", "Payment to bank account"),
                Map.entry("48", "Bank card"),
                Map.entry("49", "Direct debit"),
                Map.entry("57", "Standing agreement"),
                Map.entry("58", "SEPA credit transfer"),
                Map.entry("59", "SEPA direct debit"),
                Map.entry("68", "Online payment service"),
                Map.entry("1", "Instrument not defined")));
        put(fields, "PaymentID", "Remittance Information / Payment Reference (BT-83)");
        put(fields, "PayeeFinancialAccount/ID", "Payment Account Identifier / IBAN (BT-84)");
        put(fields, "PayeeFinancialAccount/Name", "Payment Account Name (BT-85)");
        put(fields, "PayeeFinancialAccount/FinancialInstitutionBranch/ID", "Payment Service Provider Identifier / BIC (BT-86)");
        return fields;
    }

    private static Map<String, InvoiceField> taxTotal() {
        final Map<String, InvoiceField> fields = new LinkedHashMap<>();
        put(fields, "TaxAmount", "Invoice Total VAT Amount (BT-110)");
        put(fields, "TaxSubtotal/TaxableAmount", "VAT Category Taxable Amount (BT-116)");
        put(fields, "TaxSubtotal/TaxAmount", "VAT Category Tax Amount (BT-117)");
        put(fields, "TaxSubtotal/TaxCategory/ID", "VAT Category Code (BT-118)", vatCategoryMap());
        put(fields, "TaxSubtotal/TaxCategory/Percent", "VAT Category Rate (BT-119)");
        put(fields, "TaxSubtotal/TaxCategory/TaxExemptionReason", "VAT Exemption Reason Text (BT-120)");
        put(fields, "TaxSubtotal/TaxCategory/TaxExemptionReasonCode", "VAT Exemption Reason Code (BT-121)");
        put(fields, "TaxSubtotal/TaxCategory/TaxScheme/ID", "Tax Scheme Identifier");
        return fields;
    }

    private static Map<String, InvoiceField> monetaryTotal() {
        final Map<String, InvoiceField> fields = new LinkedHashMap<>();
        put(fields, "LineExtensionAmount", "Sum of Invoice Line Net Amounts (BT-106)");
        put(fields, "AllowanceTotalAmount", "Sum of Allowances on Document Level (BT-107)");
        put(fields, "ChargeTotalAmount", "Sum of Charges on Document Level (BT-108)");
        put(fields, "TaxExclusiveAmount", "Invoice Total Amount without VAT (BT-109)");
        put(fields, "TaxInclusiveAmount", "Invoice Total Amount with VAT (BT-112)");
        put(fields, "PrepaidAmount", "Paid Amount (BT-113)");
        put(fields, "PayableRoundingAmount", "Rounding Amount (BT-114)");
        put(fields, "PayableAmount", "Amount Due for Payment (BT-115)");
        return fields;
    }

    private static Map<String, InvoiceField> invoiceLine() {
        final Map<String, InvoiceField> fields = new LinkedHashMap<>();
        put(fields, "ID", "Invoice Line Identifier (BT-126)");
        put(fields, "Note", "Invoice Line Note (BT-127)");
        put(fields, "InvoicedQuantity", "Invoiced Quantity (BT-129)");
        put(fields, "CreditedQuantity", "Credited Quantity (BT-129)");
        put(fields, "LineExtensionAmount", "Invoice Line Net Amount (BT-131)");
        put(fields, "AccountingCost", "Invoice Line Buyer Accounting Reference (BT-133)");
        put(fields, "InvoicePeriod/StartDate", "Invoice Line Period Start Date (BT-134)");
        put(fields, "InvoicePeriod/EndDate", "Invoice Line Period End Date (BT-135)");
        put(fields, "Item/Name", "Item Name (BT-153)");
        put(fields, "Item/Description", "Item Description (BT-154)");
        put(fields, "Item/SellersItemIdentification/ID", "Seller's Item Identifier (BT-155)");
        put(fields, "Item/StandardItemIdentification/ID", "Standard Item Identifier (BT-157)");
        put(fields, "Item/OriginCountry/IdentificationCode", "Item Country of Origin (BT-159)");
        put(fields, "Item/ClassifiedTaxCategory/ID", "Invoiced Item VAT Category Code (BT-151)", vatCategoryMap());
        put(fields, "Item/ClassifiedTaxCategory/Percent", "Invoiced Item VAT Rate (BT-152)");
        put(fields, "Price/PriceAmount", "Item Net Price (BT-146)");
        put(fields, "Price/BaseQuantity", "Item Price Base Quantity (BT-149)");
        return fields;
    }

    private static Map<String, String> invoiceTypeCodeMap() {
        return Map.ofEntries(
                Map.entry("380", "Commercial invoice"),
                Map.entry("381", "Credit note"),
                Map.entry("384", "Corrected invoice"),
                Map.entry("386", "Prepayment invoice"),
                Map.entry("389", "Self-billed invoice"),
                Map.entry("261", "Self billed credit note"),
                Map.entry("326", "Partial invoice"),
                Map.entry("751", "Invoice information for accounting purposes"));
    }

    private static Map<String, String> vatCategoryMap() {
        return Map.ofEntries(
                Map.entry("S", "Standard rate"),
                Map.entry("Z", "Zero rated goods"),
                Map.entry("E", "Exempt from tax"),
                Map.entry("AE", "VAT Reverse Charge"),
                Map.entry("K", "VAT exempt for EEA intra-community supply"),
                Map.entry("G", "Free export item, VAT not charged"),
                Map.entry("O", "Services outside scope of tax"),
                Map.entry("L", "Canary Islands general indirect tax"),
                Map.entry("M", "Tax for production, services and importation in Ceuta and Melilla"));
    }

    private static void put(Map<String, InvoiceField> fields, String code, String label) {
        fields.put(code, new InvoiceField(code, label));
    }

    private static void put(Map<String, InvoiceField> fields, String code, String label, Map<String, String> valueMap) {
        fields.put(code, new InvoiceField(code, label, valueMap));
    }
}
