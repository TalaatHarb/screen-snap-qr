package net.talaatharb.screensnapqr.ui.shc;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.DataFormatException;
import java.util.zip.Inflater;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Detects and decodes SMART Health Card (SHC) {@code shc:/} QR/Data Matrix
 * payloads: a numerically-encoded compact JWS whose payload segment is a
 * raw-deflate-compressed JSON Verifiable Credential wrapping a FHIR
 * {@code Bundle} (see <a href="https://smarthealth.cards">smarthealth.cards</a>).
 */
public final class ShcParser {

    private static final String PREFIX = "shc:/";
    private static final Pattern CHUNKED_PREFIX = Pattern.compile("^(\\d+)/(\\d+)/(.*)$", Pattern.DOTALL);
    private static final Pattern NUMERIC_PAYLOAD = Pattern.compile("^\\d+$");

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private ShcParser() {
    }

    /**
     * Attempts to decode the given scanned text as a single-chunk SMART Health
     * Card payload.
     *
     * @return the parsed document, or {@link Optional#empty()} if the content
     *         does not look like a (single-chunk) SHC payload, or does not
     *         decode to the expected Verifiable Credential/FHIR Bundle shape.
     */
    public static Optional<ShcDocument> tryParse(String text) {
        if (text == null) {
            return Optional.empty();
        }
        final String trimmed = text.strip();
        if (!trimmed.startsWith(PREFIX)) {
            return Optional.empty();
        }

        String numeric = trimmed.substring(PREFIX.length());
        final Matcher chunkMatcher = CHUNKED_PREFIX.matcher(numeric);
        if (chunkMatcher.matches()) {
            final int chunkIndex = Integer.parseInt(chunkMatcher.group(1));
            final int chunkCount = Integer.parseInt(chunkMatcher.group(2));
            if (chunkIndex != 1 || chunkCount != 1) {
                // Multi-chunk health cards require combining several scanned codes in
                // order before decoding; only single-chunk payloads are supported.
                return Optional.empty();
            }
            numeric = chunkMatcher.group(3);
        }

        if (!NUMERIC_PAYLOAD.matcher(numeric).matches() || numeric.length() % 2 != 0) {
            return Optional.empty();
        }

        try {
            final String jws = decodeNumeric(numeric);
            final String[] segments = jws.split("\\.", -1);
            if (segments.length != 3) {
                return Optional.empty();
            }

            final byte[] inflatedPayload = inflate(Base64.getUrlDecoder().decode(pad(segments[1])));
            final JsonNode root = OBJECT_MAPPER.readTree(new String(inflatedPayload, StandardCharsets.UTF_8));

            final JsonNode fhirBundle = root.path("vc").path("credentialSubject").path("fhirBundle");
            if (!fhirBundle.isObject() || !fhirBundle.path("entry").isArray()) {
                return Optional.empty();
            }

            return Optional.of(toShcDocument(root, fhirBundle));
        } catch (IOException | DataFormatException | RuntimeException ex) {
            return Optional.empty();
        }
    }

    private static ShcDocument toShcDocument(JsonNode root, JsonNode fhirBundle) throws IOException {
        final Map<String, String> headerAttributes = new LinkedHashMap<>();
        putIfPresent(headerAttributes, "iss", root.path("iss").isMissingNode() ? null : root.path("iss").asText());
        if (root.has("nbf")) {
            putIfPresent(headerAttributes, "nbf", ShcFormatting.formatEpochSeconds(root.path("nbf").numberValue()));
        }
        if (root.has("exp")) {
            putIfPresent(headerAttributes, "exp", ShcFormatting.formatEpochSeconds(root.path("exp").numberValue()));
        }
        final JsonNode typeArray = root.path("vc").path("type");
        if (typeArray.isArray()) {
            final List<String> types = new ArrayList<>();
            typeArray.forEach(node -> types.add(shortCredentialType(node.asText())));
            putIfPresent(headerAttributes, "types", String.join(", ", types));
        }
        final JsonNode fhirVersion = root.path("vc").path("credentialSubject").path("fhirVersion");
        if (!fhirVersion.isMissingNode()) {
            putIfPresent(headerAttributes, "fhirVersion", fhirVersion.asText());
        }

        final List<ShcDocument.ShcResource> resources = new ArrayList<>();
        for (JsonNode entry : fhirBundle.path("entry")) {
            final JsonNode resource = entry.path("resource");
            if (!resource.isObject()) {
                continue;
            }
            final String resourceType = resource.path("resourceType").asText(null);
            resources.add(ShcDocument.ShcResource.builder()
                    .resourceType(resourceType)
                    .attributes(FhirResourceSummarizer.summarize(resource))
                    .rawJson(OBJECT_MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(resource))
                    .build());
        }

        return ShcDocument.builder().headerAttributes(headerAttributes).resources(resources).build();
    }

    /** Shortens a SMART Health Cards credential type URI to its trailing fragment, e.g. {@code "#health-card"}. */
    private static String shortCredentialType(String typeUri) {
        if (typeUri == null) {
            return null;
        }
        final int hashIndex = typeUri.lastIndexOf('#');
        return hashIndex >= 0 ? typeUri.substring(hashIndex + 1) : typeUri;
    }

    /** Decodes the JWS numeric encoding: each pair of digits is (charCode - 45). */
    private static String decodeNumeric(String numeric) {
        final StringBuilder decoded = new StringBuilder(numeric.length() / 2);
        for (int i = 0; i < numeric.length(); i += 2) {
            final int value = Integer.parseInt(numeric.substring(i, i + 2));
            decoded.append((char) (value + 45));
        }
        return decoded.toString();
    }

    /** Inflates raw-deflate (no zlib/gzip header) compressed bytes, as used by the JWS payload segment. */
    private static byte[] inflate(byte[] compressed) throws DataFormatException {
        final Inflater inflater = new Inflater(true);
        try {
            inflater.setInput(compressed);
            final ByteArrayOutputStream output = new ByteArrayOutputStream(Math.max(64, compressed.length * 3));
            final byte[] buffer = new byte[4096];
            while (!inflater.finished()) {
                final int count = inflater.inflate(buffer);
                if (count == 0) {
                    if (inflater.needsInput() || inflater.needsDictionary()) {
                        break;
                    }
                }
                output.write(buffer, 0, count);
            }
            return output.toByteArray();
        } finally {
            inflater.end();
        }
    }

    /** Restores standard Base64 padding stripped from base64url-encoded JWS segments. */
    private static String pad(String base64UrlNoPadding) {
        final int remainder = base64UrlNoPadding.length() % 4;
        if (remainder == 0) {
            return base64UrlNoPadding;
        }
        return base64UrlNoPadding + "=".repeat(4 - remainder);
    }

    private static void putIfPresent(Map<String, String> map, String key, String value) {
        if (value != null && !value.isBlank()) {
            map.put(key, value);
        }
    }
}
