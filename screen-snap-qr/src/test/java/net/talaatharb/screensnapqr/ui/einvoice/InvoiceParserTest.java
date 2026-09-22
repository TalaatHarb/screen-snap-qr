package net.talaatharb.screensnapqr.ui.einvoice;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;

import org.junit.jupiter.api.Test;

import net.talaatharb.screensnapqr.ui.einvoice.InvoiceFieldDictionary.Context;

class InvoiceParserTest {

    private static final String SAMPLE_INVOICE = """
            <Invoice xmlns="urn:oasis:names:specification:ubl:schema:xsd:Invoice-2"
                     xmlns:cac="urn:oasis:names:specification:ubl:schema:xsd:CommonAggregateComponents-2"
                     xmlns:cbc="urn:oasis:names:specification:ubl:schema:xsd:CommonBasicComponents-2">
                <cbc:CustomizationID>urn:cen.eu:en16931:2017#compliant#urn:fdc:peppol.eu:2017:poacc:billing:3.0</cbc:CustomizationID>
                <cbc:ProfileID>urn:fdc:peppol.eu:2017:poacc:billing:01:1.0</cbc:ProfileID>
                <cbc:ID>INV-2024-001</cbc:ID>
                <cbc:IssueDate>2024-03-01</cbc:IssueDate>
                <cbc:DueDate>2024-03-31</cbc:DueDate>
                <cbc:InvoiceTypeCode>380</cbc:InvoiceTypeCode>
                <cbc:DocumentCurrencyCode>EUR</cbc:DocumentCurrencyCode>
                <cbc:BuyerReference>BUY-REF-1</cbc:BuyerReference>
                <cac:AccountingSupplierParty>
                    <cac:Party>
                        <cbc:EndpointID schemeID="0088">1234567890123</cbc:EndpointID>
                        <cac:PartyName><cbc:Name>Acme Supplies SRL</cbc:Name></cac:PartyName>
                        <cac:PostalAddress>
                            <cbc:StreetName>Main Street 1</cbc:StreetName>
                            <cbc:CityName>Bucharest</cbc:CityName>
                            <cac:Country><cbc:IdentificationCode>RO</cbc:IdentificationCode></cac:Country>
                        </cac:PostalAddress>
                        <cac:PartyTaxScheme><cbc:CompanyID>RO12345678</cbc:CompanyID></cac:PartyTaxScheme>
                        <cac:PartyLegalEntity>
                            <cbc:RegistrationName>Acme Supplies SRL</cbc:RegistrationName>
                            <cbc:CompanyID>J40/1234/2020</cbc:CompanyID>
                        </cac:PartyLegalEntity>
                    </cac:Party>
                </cac:AccountingSupplierParty>
                <cac:AccountingCustomerParty>
                    <cac:Party>
                        <cbc:EndpointID schemeID="0088">9876543210987</cbc:EndpointID>
                        <cac:PartyLegalEntity><cbc:RegistrationName>Beta Buyer SA</cbc:RegistrationName></cac:PartyLegalEntity>
                    </cac:Party>
                </cac:AccountingCustomerParty>
                <cac:PaymentMeans>
                    <cbc:PaymentMeansCode>58</cbc:PaymentMeansCode>
                    <cbc:PaymentID>INV-2024-001</cbc:PaymentID>
                    <cac:PayeeFinancialAccount><cbc:ID>RO00BANK000012345</cbc:ID></cac:PayeeFinancialAccount>
                </cac:PaymentMeans>
                <cac:TaxTotal>
                    <cbc:TaxAmount currencyID="EUR">19.00</cbc:TaxAmount>
                    <cac:TaxSubtotal>
                        <cbc:TaxableAmount currencyID="EUR">100.00</cbc:TaxableAmount>
                        <cbc:TaxAmount currencyID="EUR">19.00</cbc:TaxAmount>
                        <cac:TaxCategory>
                            <cbc:ID>S</cbc:ID>
                            <cbc:Percent>19.0</cbc:Percent>
                        </cac:TaxCategory>
                    </cac:TaxSubtotal>
                </cac:TaxTotal>
                <cac:LegalMonetaryTotal>
                    <cbc:LineExtensionAmount currencyID="EUR">100.00</cbc:LineExtensionAmount>
                    <cbc:TaxExclusiveAmount currencyID="EUR">100.00</cbc:TaxExclusiveAmount>
                    <cbc:TaxInclusiveAmount currencyID="EUR">119.00</cbc:TaxInclusiveAmount>
                    <cbc:PayableAmount currencyID="EUR">119.00</cbc:PayableAmount>
                </cac:LegalMonetaryTotal>
                <cac:InvoiceLine>
                    <cbc:ID>1</cbc:ID>
                    <cbc:InvoicedQuantity unitCode="C62">2</cbc:InvoicedQuantity>
                    <cbc:LineExtensionAmount currencyID="EUR">100.00</cbc:LineExtensionAmount>
                    <cac:Item><cbc:Name>Widget</cbc:Name></cac:Item>
                    <cac:Price><cbc:PriceAmount currencyID="EUR">50.00</cbc:PriceAmount></cac:Price>
                </cac:InvoiceLine>
            </Invoice>
            """;

    @Test
    void testParsesInvoiceIntoStructuredDocument() {
        final Optional<InvoiceDocument> parsed = InvoiceParser.tryParse(SAMPLE_INVOICE);

        assertTrue(parsed.isPresent());
        final InvoiceDocument document = parsed.get();
        assertEquals("Invoice", document.getRootElementName());
        assertFalse(document.isCreditNote());
        assertEquals("INV-2024-001", document.getHeaderFields().get("ID"));
        assertEquals("380", document.getHeaderFields().get("InvoiceTypeCode"));

        assertEquals("Acme Supplies SRL", document.getSupplierParty().get("PartyName/Name"));
        assertEquals("RO12345678", document.getSupplierParty().get("PartyTaxScheme/CompanyID"));
        assertEquals("J40/1234/2020", document.getSupplierParty().get("PartyLegalEntity/CompanyID"));

        assertEquals("Beta Buyer SA", document.getCustomerParty().get("PartyLegalEntity/RegistrationName"));

        assertEquals("58", document.getPaymentMeans().get("PaymentMeansCode"));
        assertEquals("19.00 EUR", document.getTaxTotal().get("TaxAmount"));
        assertEquals("119.00 EUR", document.getMonetaryTotal().get("PayableAmount"));

        assertEquals(1, document.getLines().size());
        assertEquals("Widget", document.getLines().get(0).get("Item/Name"));
        assertEquals("2 C62", document.getLines().get(0).get("InvoicedQuantity"));
    }

    @Test
    void testReturnsEmptyForUnrelatedXml() {
        final Optional<InvoiceDocument> parsed = InvoiceParser
                .tryParse("<bean id=\"foo\" class=\"com.example.Foo\" scope=\"singleton\"/>");
        assertFalse(parsed.isPresent());
    }

    @Test
    void testReturnsEmptyForNonXmlText() {
        assertFalse(InvoiceParser.tryParse("not xml at all").isPresent());
        assertFalse(InvoiceParser.tryParse(null).isPresent());
        assertFalse(InvoiceParser.tryParse("").isPresent());
    }

    @Test
    void testReturnsEmptyWhenRootLacksSignatureElements() {
        final Optional<InvoiceDocument> parsed = InvoiceParser
                .tryParse("<Invoice xmlns=\"urn:oasis:names:specification:ubl:schema:xsd:Invoice-2\"><ID>1</ID></Invoice>");
        assertFalse(parsed.isPresent());
    }

    @Test
    void testDictionaryResolvesEnumeratedValuesPerElementContext() {
        assertEquals("Commercial invoice", InvoiceFieldDictionary.describeValue(Context.HEADER, "InvoiceTypeCode", "380"));
        assertEquals("Standard rate",
                InvoiceFieldDictionary.describeValue(Context.TAX_TOTAL, "TaxSubtotal/TaxCategory/ID", "S"));
        assertEquals("Seller VAT Identifier (BT-31)",
                InvoiceFieldDictionary.getLabel(Context.SUPPLIER_PARTY, "PartyTaxScheme/CompanyID"));
        assertEquals("Seller Legal Registration Identifier (BT-30)",
                InvoiceFieldDictionary.getLabel(Context.SUPPLIER_PARTY, "PartyLegalEntity/CompanyID"));
    }
}
