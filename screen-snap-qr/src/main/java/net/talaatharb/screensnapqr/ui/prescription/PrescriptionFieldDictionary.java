package net.talaatharb.screensnapqr.ui.prescription;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Static catalog of the attribute codes defined by the Romanian e-prescription
 * 2D barcode schema (PEBarcode.xsd, namespace http://www.cnas.ro/pel/1.0).
 * <p>
 * Attribute codes are only unique within the element that declares them (for
 * example {@code CT} means "Insurance Contract Type" on the document header
 * but "Patient Country Code" on the {@code PD} element), so lookups are
 * scoped by {@link Context}.
 */
public final class PrescriptionFieldDictionary {

    /** Element scopes in which attribute codes are declared by the schema. */
    public enum Context {
        /** Root element attributes, shared by both {@code <P>} and {@code <O>}. */
        HEADER,
        /** {@code <PD>} - Prescription details (patient and prescriber). */
        PRESCRIPTION_DETAILS,
        /** {@code <FD>} - Pharmacy details. */
        PHARMACY_DETAILS,
        /** {@code <D>} - Prescribed medication. */
        PRESCRIBED_MEDICATION,
        /** {@code <E>} - Dispensed medication. */
        DISPENSED_MEDICATION
    }

    private static final Map<Context, Map<String, PrescriptionField>> DICTIONARY = buildDictionary();

    private PrescriptionFieldDictionary() {
    }

    public static PrescriptionField getField(Context context, String code) {
        final Map<String, PrescriptionField> fields = DICTIONARY.get(context);
        if (fields == null) {
            return null;
        }
        return fields.get(code);
    }

    public static String getLabel(Context context, String code) {
        final PrescriptionField field = getField(context, code);
        return field != null ? field.getLabel() : code;
    }

    public static String describeValue(Context context, String code, String rawValue) {
        final PrescriptionField field = getField(context, code);
        return field != null ? field.describeValue(rawValue) : rawValue;
    }

    private static Map<Context, Map<String, PrescriptionField>> buildDictionary() {
        final Map<Context, Map<String, PrescriptionField>> dictionary = new LinkedHashMap<>();
        dictionary.put(Context.HEADER, header());
        dictionary.put(Context.PRESCRIPTION_DETAILS, prescriptionDetails());
        dictionary.put(Context.PHARMACY_DETAILS, pharmacyDetails());
        dictionary.put(Context.PRESCRIBED_MEDICATION, prescribedMedication());
        dictionary.put(Context.DISPENSED_MEDICATION, dispensedMedication());
        return dictionary;
    }

    private static Map<String, PrescriptionField> header() {
        final Map<String, PrescriptionField> fields = new LinkedHashMap<>();
        put(fields, "ID", "Date of Issue (Data prescrierii)");
        put(fields, "SC", "Prescription Series (Seria rețetei)");
        put(fields, "SN", "Prescription Number (Numărul rețetei)");
        put(fields, "PS", "Prescriber Stamp ID (Parafa medicului prescriptor)");
        put(fields, "CC", "Medical Facility Code (Codul unic al unității medicale)");
        put(fields, "CN", "Contract Number (Numărul contractului medic-CAS)");
        put(fields, "CT", "Insurance Contract Type (Tipul contractului)");
        put(fields, "OU", "Health Insurance House Code (Codul casei de asigurări)");
        put(fields, "AK", "Reporting App Key (Cheia de identificare a aplicației de raportare)");
        return fields;
    }

    private static Map<String, PrescriptionField> prescriptionDetails() {
        final Map<String, PrescriptionField> fields = new LinkedHashMap<>();
        put(fields, "S", "Prescription Source (Sursă rețetă)");
        put(fields, "PT", "Patient Type (Tipul pacientului)");
        put(fields, "NA", "PNS Number - insured basis, chapter 2 (Numar PNS cap. 2)");
        put(fields, "N", "PNS Number - patient enrollment, chapter 3 (Numar PNS cap. 3)");
        put(fields, "ON", "Recommending Doctor Stamp (Parafă medic recomandare)");
        put(fields, "PP", "Prescriber Phone Number (Numărul de telefon al medicului prescriptor)");
        put(fields, "PE", "Prescriber Email (Adresa de e-mail a medicului prescriptor)");
        put(fields, "PA", "Prescriber Postal Address (Adresa poștală a medicului prescriptor)");
        put(fields, "TY", "Prescription Type (Tipul rețetei)", Map.of(
                "E", "Electronic prescription",
                "EV", "Electronic prescription (cost/volume)",
                "ER", "Electronic prescription (cost/volume/result)"));
        put(fields, "CD", "Insured Patient CID (CID asigurat)");
        put(fields, "FN", "Patient First Name (Prenume asigurat)");
        put(fields, "LN", "Patient Last Name (Nume asigurat)");
        put(fields, "FA", "Patient Address (Adresă asigurat)");
        put(fields, "CT", "Patient Country Code (Cod țară asigurat)");
        put(fields, "AC", "Approved by Commission (Rețetă aprobată de comisie)", booleanMap());
        put(fields, "DD", "Treatment File Disease Code (Cod boală dosar de tratament)");
        put(fields, "DN", "Treatment File Approval Decision Number (Numărul deciziei de aprobare a dosarului)");
        put(fields, "DA", "Treatment File Approval Decision Date (Data deciziei de aprobare a dosarului)");
        put(fields, "SL", "Special Law Number for 100% Compensation (Numărul legii speciale)");
        put(fields, "MR", "Medical Records Registration Number (Nr. înregistrare fișă de observații)");
        put(fields, "DT", "Treatment Days (Număr zile tratament)");
        put(fields, "RO", "Reporting Mode (Mod de raportare)", Map.of(
                "0", "Offline",
                "1", "Online"));
        put(fields, "IT", "Dispensing Status (Tip eliberare rețetă)", Map.of(
                "0", "Prescribed / Not dispensed",
                "1", "Fully dispensed",
                "2", "Partially dispensed",
                "3", "Fractionally dispensed",
                "4", "Partially and fractionally dispensed"));
        put(fields, "SX", "Patient Sex (Sex pacient)");
        put(fields, "BD", "Patient Birth Date (Data nașterii pacientului)");
        put(fields, "FP", "Foreign Citizen Indicator (Indicator cetățean străin)", booleanMap());
        put(fields, "EF", "European Form Code (Cod formular european)");
        put(fields, "SS", "Passport Number (Numărul Pasaportului)");
        put(fields, "CE", "European Health Card Number (Numărul Cardului European)");
        put(fields, "NS", "Drug-Rights Document Number/Series (Nr. și serie document drept la medicamente)");
        put(fields, "NT", "Drug-Rights Document Type (Tip document drept la medicamente)");
        return fields;
    }

    private static Map<String, PrescriptionField> pharmacyDetails() {
        final Map<String, PrescriptionField> fields = new LinkedHashMap<>();
        put(fields, "CC", "Pharmacy Fiscal Code (Codul fiscal al farmaciei)");
        put(fields, "OU", "Insurance House Code (Codul casei de asigurări)");
        put(fields, "CN", "Pharmacy Contract Number (Număr de contract al farmaciei)");
        put(fields, "AC", "Authorized Person CID (CID împuternicit)");
        put(fields, "RD", "Dispensing Date (Data eliberării rețetei)");
        put(fields, "R", "Receipt Number (Număr chitanță)");
        put(fields, "A", "Approved Decision Contains Drug Name (Decizie aprobată conține numele medicamentului)", booleanMap());
        return fields;
    }

    private static Map<String, PrescriptionField> prescribedMedication() {
        final Map<String, PrescriptionField> fields = new LinkedHashMap<>();
        put(fields, "CD", "Prescribed Drug Code (Codul medicamentului prescris)");
        put(fields, "DC", "Prescribing Disease Code (Codul de boală pentru care se prescrie)");
        put(fields, "CM", "CIM-10 Disease Code (Codul de boală din nomenclatorul CIM10)");
        put(fields, "DO", "Disease Category (Categoria bolii)");
        put(fields, "DT", "Diagnostic Type Code (Codul tipului de diagnostic)");
        put(fields, "LT", "Compensated List Code (Cod listă compensată)");
        put(fields, "LP", "Compensation Percentage (Procent de compensare)");
        put(fields, "IC", "Treatment Type (Tip prescriere)", Map.of(
                "0", "Continuation",
                "1", "Initial"));
        put(fields, "TM", "Prescribed Months (Număr luni prescrise)");
        put(fields, "TP", "Patient Type (Tip pacient)", Map.of(
                "0", "Adult",
                "1", "Child"));
        put(fields, "CN", "Batch Position (Poziția rețetei în borderou)");
        put(fields, "BP", "Protocol-Based Dispensing (Indicator eliberare pe bază de protocol)", booleanMap());
        put(fields, "AS", "Active Substance Code (Cod substanță activă / DCI)");
        put(fields, "C", "Active Substance Concentration (Concentrația de substanță activă)");
        put(fields, "PF", "Pharmaceutical Form (Formă farmaceutică)");
        put(fields, "D", "Prescribed Dose (Doză medicament prescris)");
        put(fields, "Q", "Prescribed Quantity (Cantitate prescrisă)");
        put(fields, "M", "Medical Justification for Brand Name (Motivația medicală)");
        put(fields, "TC", "Consumable Type (Tipul de consumabil)", Map.of(
                "0", "Goods",
                "2", "Medication",
                "4", "Medical tests",
                "5", "Medical devices"));
        put(fields, "IT", "Dispensing Status (Tipul de eliberare a medicamentului)", Map.of(
                "0", "Prescribed / Not dispensed",
                "1", "Fully dispensed",
                "2", "Cancelled",
                "3", "Fractionally dispensed"));
        put(fields, "TD", "Prescribed Treatment Days (Numărul de zile de tratament prescrise)");
        return fields;
    }

    private static Map<String, PrescriptionField> dispensedMedication() {
        final Map<String, PrescriptionField> fields = new LinkedHashMap<>();
        put(fields, "D", "Dispensed Drug Code (Codul medicamentului eliberat)");
        put(fields, "RP", "Retail Unit Price (Prețul unitar cu amănuntul)");
        put(fields, "PR", "Reference Price (Prețul de referință)");
        put(fields, "RV", "Retail Value (Valoarea cu amănuntul)");
        put(fields, "CV", "Compensated Value (Valoarea compensată)");
        put(fields, "CV4", "Compensated Value - 40% MS budget share, pensioners < 600 lei/month");
        put(fields, "CV5", "Compensated Value - 50% FNUASS budget share, pensioners < 600 lei/month");
        put(fields, "VP", "VAT Percentage Value (Valoare procent TVA)");
        put(fields, "PV", "Protocol-Based Medication Value (Valoare medicament pe bază de protocol)");
        put(fields, "QP", "Package Quantity (Cantitate de medicament pe forma de ambalare)");
        put(fields, "PP", "Package Price (Preț medicament pe forma de ambalare)");
        put(fields, "V", "Amount Paid by Insured (Valoare plătită de către asigurat)");
        put(fields, "IQ", "Dispensed Quantity (Cantitate eliberată)");
        put(fields, "TD", "Dispensed Treatment Days (Numărul de zile de tratament eliberate)");
        return fields;
    }

    private static Map<String, String> booleanMap() {
        return Map.of("true", "Yes", "1", "Yes", "false", "No", "0", "No");
    }

    private static void put(Map<String, PrescriptionField> fields, String code, String label) {
        fields.put(code, new PrescriptionField(code, label));
    }

    private static void put(Map<String, PrescriptionField> fields, String code, String label, Map<String, String> valueMap) {
        fields.put(code, new PrescriptionField(code, label, valueMap));
    }
}
