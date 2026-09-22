package net.talaatharb.screensnapqr.ui.shc;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Static catalog of human-readable labels for SMART Health Card (SHC) header
 * attributes and the FHIR resource fields most commonly carried by health
 * cards (Patient, Immunization, Observation, Condition), plus a small CVX
 * vaccine code lookup used to describe {@code Immunization.vaccineCode}.
 * <p>
 * Field codes are only unique within a given resource type (for example
 * {@code code} means different things on an {@code Observation} vs a
 * {@code Condition}), so lookups are scoped by {@link Context}.
 */
public final class ShcFieldDictionary {

    /** Scopes in which field codes are declared. */
    public enum Context {
        /** Health card header (issuer, dates, credential types, FHIR version). */
        HEADER,
        /** FHIR {@code Patient} resource. */
        PATIENT,
        /** FHIR {@code Immunization} resource. */
        IMMUNIZATION,
        /** FHIR {@code Observation} resource (e.g. lab results). */
        OBSERVATION,
        /** FHIR {@code Condition} resource. */
        CONDITION,
        /** Any other FHIR resource type, described generically. */
        GENERIC_RESOURCE
    }

    /** Common CVX vaccine codes (https://www2a.cdc.gov/vaccines/iis/iisstandards/vaccines.asp). */
    private static final Map<String, String> CVX_VACCINES = cvxVaccines();

    private static final Map<Context, Map<String, ShcField>> DICTIONARY = buildDictionary();

    private ShcFieldDictionary() {
    }

    public static String getLabel(Context context, String code) {
        final Map<String, ShcField> fields = DICTIONARY.get(context);
        final ShcField field = fields != null ? fields.get(code) : null;
        return field != null ? field.getLabel() : code;
    }

    public static String describeValue(Context context, String code, String rawValue) {
        final Map<String, ShcField> fields = DICTIONARY.get(context);
        final ShcField field = fields != null ? fields.get(code) : null;
        return field != null ? field.describeValue(rawValue) : rawValue;
    }

    /** Resolves a CVX vaccine code to its common name, falling back to the raw code. */
    public static String describeCvxCode(String cvxCode) {
        if (cvxCode == null) {
            return null;
        }
        return CVX_VACCINES.getOrDefault(cvxCode, cvxCode);
    }

    private static Map<Context, Map<String, ShcField>> buildDictionary() {
        final Map<Context, Map<String, ShcField>> dictionary = new LinkedHashMap<>();
        dictionary.put(Context.HEADER, header());
        dictionary.put(Context.PATIENT, patient());
        dictionary.put(Context.IMMUNIZATION, immunization());
        dictionary.put(Context.OBSERVATION, observation());
        dictionary.put(Context.CONDITION, condition());
        return dictionary;
    }

    private static Map<String, ShcField> header() {
        final Map<String, ShcField> fields = new LinkedHashMap<>();
        put(fields, "iss", "Issuer");
        put(fields, "nbf", "Issued At");
        put(fields, "exp", "Expires At");
        put(fields, "types", "Credential Type(s)");
        put(fields, "fhirVersion", "FHIR Version");
        return fields;
    }

    private static Map<String, ShcField> patient() {
        final Map<String, ShcField> fields = new LinkedHashMap<>();
        put(fields, "name", "Name");
        put(fields, "birthDate", "Date of Birth");
        put(fields, "gender", "Gender");
        return fields;
    }

    private static Map<String, ShcField> immunization() {
        final Map<String, ShcField> fields = new LinkedHashMap<>();
        put(fields, "status", "Status");
        put(fields, "vaccineCode", "Vaccine");
        put(fields, "occurrenceDateTime", "Date Given");
        put(fields, "lotNumber", "Lot Number");
        put(fields, "performer", "Administered By");
        put(fields, "doseNumber", "Dose Number");
        put(fields, "seriesDoses", "Doses in Series");
        return fields;
    }

    private static Map<String, ShcField> observation() {
        final Map<String, ShcField> fields = new LinkedHashMap<>();
        put(fields, "status", "Status");
        put(fields, "code", "Test/Observation");
        put(fields, "value", "Result");
        put(fields, "effectiveDateTime", "Date");
        return fields;
    }

    private static Map<String, ShcField> condition() {
        final Map<String, ShcField> fields = new LinkedHashMap<>();
        put(fields, "code", "Condition");
        put(fields, "clinicalStatus", "Clinical Status");
        put(fields, "onsetDateTime", "Onset Date");
        return fields;
    }

    private static Map<String, String> cvxVaccines() {
        final Map<String, String> vaccines = new LinkedHashMap<>();
        vaccines.put("207", "COVID-19 (Moderna)");
        vaccines.put("208", "COVID-19 (Pfizer-BioNTech)");
        vaccines.put("210", "COVID-19 (AstraZeneca)");
        vaccines.put("211", "COVID-19 (Novavax)");
        vaccines.put("212", "COVID-19 (Janssen/J&J)");
        vaccines.put("213", "COVID-19 (unspecified formulation)");
        vaccines.put("217", "COVID-19 (Pfizer-BioNTech, bivalent)");
        vaccines.put("219", "COVID-19 (Moderna, bivalent)");
        vaccines.put("88", "Influenza (unspecified formulation)");
        vaccines.put("140", "Influenza, seasonal, injectable");
        vaccines.put("03", "MMR (Measles, Mumps, Rubella)");
        vaccines.put("94", "MMRV (Measles, Mumps, Rubella, Varicella)");
        vaccines.put("21", "Varicella");
        vaccines.put("08", "Hepatitis B, pediatric/adolescent");
        vaccines.put("43", "Hepatitis B, adult");
        vaccines.put("133", "Pneumococcal conjugate PCV 13");
        return vaccines;
    }

    private static void put(Map<String, ShcField> fields, String code, String label) {
        fields.put(code, new ShcField(code, label));
    }
}
