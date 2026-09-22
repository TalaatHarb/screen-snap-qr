package net.talaatharb.screensnapqr.ui.prescription;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import net.talaatharb.screensnapqr.ui.prescription.PrescriptionFieldDictionary.Context;

/**
 * Converts a parsed {@link PrescriptionDocument} into shareable, human-readable
 * text and JSON representations (used both for clipboard copy and file export).
 */
public final class PrescriptionExporter {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private PrescriptionExporter() {
    }

    /** Renders the document as a labeled, plain-text report. */
    public static String toPlainText(PrescriptionDocument document) {
        final StringBuilder text = new StringBuilder();
        text.append(document.isOnline() ? "Online Prescription (<P>)" : "Offline Prescription (<O>)").append(System.lineSeparator())
                .append(System.lineSeparator());

        appendSection(text, "Header", Context.HEADER, document.getHeaderAttributes());
        if (document.getPrescriptionDetails() != null) {
            appendSection(text, "Prescription Details (Patient & Prescriber)", Context.PRESCRIPTION_DETAILS,
                    document.getPrescriptionDetails());
        }
        if (document.getPharmacyDetails() != null) {
            appendSection(text, "Pharmacy Details", Context.PHARMACY_DETAILS, document.getPharmacyDetails());
        }
        appendMedicationSection(text, "Prescribed Medications", Context.PRESCRIBED_MEDICATION,
                document.getPrescribedMedications(), "AS", "CD");
        appendMedicationSection(text, "Dispensed Medications", Context.DISPENSED_MEDICATION,
                document.getDispensedMedications(), "D");

        return text.toString().stripTrailing() + System.lineSeparator();
    }

    /** Renders the document as pretty-printed JSON, using resolved human-readable labels as keys. */
    public static String toJson(PrescriptionDocument document) throws JsonProcessingException {
        final Map<String, Object> root = new LinkedHashMap<>();
        root.put("type", document.isOnline() ? "Online Prescription" : "Offline Prescription");
        root.put("header", describeAttributes(Context.HEADER, document.getHeaderAttributes()));
        if (document.getPrescriptionDetails() != null) {
            root.put("prescriptionDetails", describeAttributes(Context.PRESCRIPTION_DETAILS, document.getPrescriptionDetails()));
        }
        if (document.getPharmacyDetails() != null) {
            root.put("pharmacyDetails", describeAttributes(Context.PHARMACY_DETAILS, document.getPharmacyDetails()));
        }
        root.put("prescribedMedications", document.getPrescribedMedications().stream()
                .map(medication -> describeAttributes(Context.PRESCRIBED_MEDICATION, medication)).toList());
        root.put("dispensedMedications", document.getDispensedMedications().stream()
                .map(medication -> describeAttributes(Context.DISPENSED_MEDICATION, medication)).toList());

        return OBJECT_MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(root);
    }

    private static void appendSection(StringBuilder text, String title, Context context, Map<String, String> attributes) {
        text.append("== ").append(title).append(" ==").append(System.lineSeparator());
        for (Map.Entry<String, String> attribute : attributes.entrySet()) {
            text.append(PrescriptionFieldDictionary.getLabel(context, attribute.getKey())).append(": ")
                    .append(PrescriptionFieldDictionary.describeValue(context, attribute.getKey(), attribute.getValue()))
                    .append(System.lineSeparator());
        }
        text.append(System.lineSeparator());
    }

    private static void appendMedicationSection(StringBuilder text, String title, Context context,
            List<Map<String, String>> medications, String... titleAttributeCandidates) {
        if (medications.isEmpty()) {
            return;
        }
        text.append("== ").append(title).append(" (").append(medications.size()).append(") ==").append(System.lineSeparator());
        int index = 1;
        for (Map<String, String> medication : medications) {
            text.append("-- ").append(PrescriptionFormatting.medicationTitle(index, medication, titleAttributeCandidates))
                    .append(" --").append(System.lineSeparator());
            for (Map.Entry<String, String> attribute : medication.entrySet()) {
                text.append("  ").append(PrescriptionFieldDictionary.getLabel(context, attribute.getKey())).append(": ")
                        .append(PrescriptionFieldDictionary.describeValue(context, attribute.getKey(), attribute.getValue()))
                        .append(System.lineSeparator());
            }
            index++;
        }
        text.append(System.lineSeparator());
    }

    private static Map<String, String> describeAttributes(Context context, Map<String, String> attributes) {
        final Map<String, String> described = new LinkedHashMap<>();
        for (Map.Entry<String, String> attribute : attributes.entrySet()) {
            described.put(PrescriptionFieldDictionary.getLabel(context, attribute.getKey()),
                    PrescriptionFieldDictionary.describeValue(context, attribute.getKey(), attribute.getValue()));
        }
        return described;
    }
}
