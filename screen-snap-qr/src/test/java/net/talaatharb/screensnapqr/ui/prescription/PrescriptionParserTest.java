package net.talaatharb.screensnapqr.ui.prescription;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;

import org.junit.jupiter.api.Test;

import net.talaatharb.screensnapqr.ui.prescription.PrescriptionFieldDictionary.Context;

class PrescriptionParserTest {

    private static final String SAMPLE_ONLINE_PRESCRIPTION = """
            <P xmlns="http://www.cnas.ro/pel/1.0" ID="2024-01-15" SC="AB" SN="1234567" PS="12345" CC="999888"
               CN="CT-01" CT="1" OU="CASMB">
                <PD S="1" PT="0" TY="E" CD="CID12345" FN="Ion" LN="Popescu" CT="RO" AC="false"
                    DT="10" RO="1" IT="1" SX="M" BD="1980-05-01" FP="false"/>
                <FD CC="777666" OU="CASMB" CN="FCT-9" RD="2024-01-16" A="false"/>
                <D CD="W12345" CM="J00" DT="1" LT="A" LP="90.0" IC="1" TP="0" CN="1" BP="false"
                   AS="Paracetamolum" C="500 mg" PF="comprimate" Q="20" TC="2" IT="1" TD="5"/>
                <E D="W12345" RP="12.5" RV="12.5" CV="11.25" IQ="20" TD="5"/>
            </P>
            """;

    @Test
    void testParsesOnlinePrescriptionIntoStructuredDocument() {
        final Optional<PrescriptionDocument> parsed = PrescriptionParser.tryParse(SAMPLE_ONLINE_PRESCRIPTION);

        assertTrue(parsed.isPresent());
        final PrescriptionDocument document = parsed.get();
        assertEquals("P", document.getRootElementName());
        assertTrue(document.isOnline());
        assertEquals("AB", document.getHeaderAttributes().get("SC"));
        assertEquals("1234567", document.getHeaderAttributes().get("SN"));

        assertEquals("Ion", document.getPrescriptionDetails().get("FN"));
        assertEquals("777666", document.getPharmacyDetails().get("CC"));

        assertEquals(1, document.getPrescribedMedications().size());
        assertEquals("Paracetamolum", document.getPrescribedMedications().get(0).get("AS"));

        assertEquals(1, document.getDispensedMedications().size());
        assertEquals("W12345", document.getDispensedMedications().get(0).get("D"));
    }

    @Test
    void testReturnsEmptyForUnrelatedXml() {
        final Optional<PrescriptionDocument> parsed = PrescriptionParser
                .tryParse("<bean id=\"foo\" class=\"com.example.Foo\" scope=\"singleton\"/>");
        assertFalse(parsed.isPresent());
    }

    @Test
    void testReturnsEmptyForNonXmlText() {
        assertFalse(PrescriptionParser.tryParse("not xml at all").isPresent());
        assertFalse(PrescriptionParser.tryParse(null).isPresent());
        assertFalse(PrescriptionParser.tryParse("").isPresent());
    }

    @Test
    void testReturnsEmptyWhenRootLacksSignatureAttributes() {
        final Optional<PrescriptionDocument> parsed = PrescriptionParser
                .tryParse("<P xmlns=\"http://www.cnas.ro/pel/1.0\" ID=\"2024-01-15\"/>");
        assertFalse(parsed.isPresent());
    }

    @Test
    void testDictionaryResolvesEnumeratedValuesPerElementContext() {
        assertEquals("Online", PrescriptionFieldDictionary.describeValue(Context.PRESCRIPTION_DETAILS, "RO", "1"));
        assertEquals("Adult", PrescriptionFieldDictionary.describeValue(Context.PRESCRIBED_MEDICATION, "TP", "0"));
        assertEquals("Patient Type (Tipul pacientului)", PrescriptionFieldDictionary.getLabel(Context.PRESCRIPTION_DETAILS, "PT"));
        assertEquals("Batch Position (Poziția rețetei în borderou)",
                PrescriptionFieldDictionary.getLabel(Context.PRESCRIBED_MEDICATION, "CN"));
        assertEquals("Contract Number (Numărul contractului medic-CAS)",
                PrescriptionFieldDictionary.getLabel(Context.HEADER, "CN"));
    }
}
