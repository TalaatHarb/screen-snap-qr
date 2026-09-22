package net.talaatharb.screensnapqr.ui.shc;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import lombok.Builder;
import lombok.Getter;

/**
 * Parsed representation of a SMART Health Card (SHC) {@code shc:/} QR/Data
 * Matrix payload: a compact JWS whose payload is a deflate-compressed FHIR
 * {@code Bundle} carrying vaccination/lab-result resources.
 */
@Getter
@Builder
public class ShcDocument {

    /** Header attributes (issuer, issued/expiry dates, credential types, FHIR version). */
    @Builder.Default
    private final Map<String, String> headerAttributes = new LinkedHashMap<>();

    /** One entry per FHIR resource carried in {@code vc.credentialSubject.fhirBundle.entry}. */
    @Builder.Default
    private final List<ShcResource> resources = new ArrayList<>();

    /**
     * A single FHIR resource extracted from the health card bundle, with a
     * human-readable summary of its most relevant fields plus the original
     * pretty-printed JSON for full detail/export.
     */
    @Getter
    @Builder
    public static class ShcResource {
        private final String resourceType;
        @Builder.Default
        private final Map<String, String> attributes = new LinkedHashMap<>();
        private final String rawJson;
    }
}
