package net.talaatharb.screensnapqr.ui.shc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Optional;
import java.util.zip.Deflater;

import org.junit.jupiter.api.Test;

class ShcParserTest {

    private static final String SAMPLE_JSON = """
            {"iss":"https://example.org/issuer","nbf":1700000000,"vc":{"type":\
            ["https://smarthealth.cards#health-card","https://smarthealth.cards#immunization",\
            "https://smarthealth.cards#covid19"],"credentialSubject":{"fhirVersion":"4.0.1",\
            "fhirBundle":{"resourceType":"Bundle","type":"collection","entry":[\
            {"fullUrl":"resource:0","resource":{"resourceType":"Patient","name":\
            [{"family":"Anyperson","given":["John","B."]}],"birthDate":"1951-01-20"}},\
            {"fullUrl":"resource:1","resource":{"resourceType":"Immunization","status":"completed",\
            "vaccineCode":{"coding":[{"system":"http://hl7.org/fhir/sid/cvx","code":"207"}]},\
            "patient":{"reference":"resource:0"},"occurrenceDateTime":"2021-01-01","lotNumber":"0000001",\
            "performer":[{"actor":{"display":"ABC General Hospital"}}]}}]}}}}\
            """;

    @Test
    void testDecodesSingleChunkHealthCardIntoStructuredDocument() {
        final String shcPayload = encodeAsShc(SAMPLE_JSON);

        final Optional<ShcDocument> parsed = ShcParser.tryParse(shcPayload);
        assertTrue(parsed.isPresent());

        final ShcDocument document = parsed.get();
        assertEquals("https://example.org/issuer", document.getHeaderAttributes().get("iss"));
        assertEquals("4.0.1", document.getHeaderAttributes().get("fhirVersion"));
        assertEquals("health-card, immunization, covid19", document.getHeaderAttributes().get("types"));

        assertEquals(2, document.getResources().size());
        final ShcDocument.ShcResource patient = document.getResources().get(0);
        assertEquals("Patient", patient.getResourceType());
        assertEquals("John B. Anyperson", patient.getAttributes().get("name"));
        assertEquals("1951-01-20", patient.getAttributes().get("birthDate"));

        final ShcDocument.ShcResource immunization = document.getResources().get(1);
        assertEquals("Immunization", immunization.getResourceType());
        assertEquals("COVID-19 (Moderna)", immunization.getAttributes().get("vaccineCode"));
        assertEquals("0000001", immunization.getAttributes().get("lotNumber"));
        assertEquals("ABC General Hospital", immunization.getAttributes().get("performer"));
    }

    @Test
    void testAcceptsExplicitSingleChunkPrefix() {
        final String shcPayload = "shc:/1/1/" + encodeAsShc(SAMPLE_JSON).substring("shc:/".length());
        assertTrue(ShcParser.tryParse(shcPayload).isPresent());
    }

    @Test
    void testRejectsMultiChunkPayloads() {
        final String shcPayload = "shc:/1/2/" + encodeAsShc(SAMPLE_JSON).substring("shc:/".length());
        assertFalse(ShcParser.tryParse(shcPayload).isPresent());
    }

    @Test
    void testReturnsEmptyForNonShcText() {
        assertFalse(ShcParser.tryParse("not a health card").isPresent());
        assertFalse(ShcParser.tryParse(null).isPresent());
        assertFalse(ShcParser.tryParse("").isPresent());
        assertFalse(ShcParser.tryParse("<Invoice/>").isPresent());
    }

    @Test
    void testReturnsEmptyForMalformedNumericPayload() {
        assertFalse(ShcParser.tryParse("shc:/12X45").isPresent());
        assertFalse(ShcParser.tryParse("shc:/123").isPresent());
    }

    /** Mirrors {@code ShcParser}'s decoding scheme in reverse, to build a test fixture payload. */
    private static String encodeAsShc(String json) {
        try {
            final byte[] jsonBytes = json.getBytes(StandardCharsets.UTF_8);
            final Deflater deflater = new Deflater(Deflater.BEST_COMPRESSION, true);
            deflater.setInput(jsonBytes);
            deflater.finish();
            final ByteArrayOutputStream deflated = new ByteArrayOutputStream();
            final byte[] buffer = new byte[4096];
            while (!deflater.finished()) {
                final int count = deflater.deflate(buffer);
                deflated.write(buffer, 0, count);
            }
            deflater.end();

            final String payload = Base64.getUrlEncoder().withoutPadding().encodeToString(deflated.toByteArray());
            final String header = Base64.getUrlEncoder().withoutPadding()
                    .encodeToString("{\"alg\":\"ES256\",\"zip\":\"DEF\",\"kid\":\"test\"}".getBytes(StandardCharsets.UTF_8));
            final String signature = Base64.getUrlEncoder().withoutPadding()
                    .encodeToString("fakesignature".getBytes(StandardCharsets.UTF_8));
            final String jws = header + "." + payload + "." + signature;

            final StringBuilder numeric = new StringBuilder();
            for (char c : jws.toCharArray()) {
                numeric.append(String.format("%02d", (int) c - 45));
            }
            return "shc:/" + numeric;
        } catch (RuntimeException ex) {
            throw new IllegalStateException("Failed to build SHC test fixture", ex);
        }
    }
}
