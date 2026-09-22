package net.talaatharb.screensnapqr.ui.prescription;

import java.util.List;
import java.util.Map;

import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TitledPane;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import net.talaatharb.screensnapqr.ui.prescription.PrescriptionFieldDictionary.Context;

/**
 * Builds a human-readable JavaFX visualization of a parsed
 * {@link PrescriptionDocument}, resolving raw two-letter attribute codes to
 * their meaning via {@link PrescriptionFieldDictionary}.
 */
public final class PrescriptionView {

    private PrescriptionView() {
    }

    public static Node build(PrescriptionDocument document) {
        final VBox container = new VBox(10);
        container.getStyleClass().add("prescription-view");
        container.setPadding(new Insets(10));

        final String kind = document.isOnline() ? "Online Prescription (<P>)" : "Offline Prescription (<O>)";
        final Label title = new Label(kind);
        title.getStyleClass().add("prescription-title");
        container.getChildren().add(title);

        container.getChildren().add(section("Header", Context.HEADER, document.getHeaderAttributes()));

        if (document.getPrescriptionDetails() != null) {
            container.getChildren()
                    .add(section("Prescription Details (Patient & Prescriber)", Context.PRESCRIPTION_DETAILS,
                            document.getPrescriptionDetails()));
        }

        if (document.getPharmacyDetails() != null) {
            container.getChildren().add(section("Pharmacy Details", Context.PHARMACY_DETAILS, document.getPharmacyDetails()));
        }

        if (!document.getPrescribedMedications().isEmpty()) {
            container.getChildren().add(medicationSection("Prescribed Medications", Context.PRESCRIBED_MEDICATION,
                    document.getPrescribedMedications(), "AS", "CD"));
        }

        if (!document.getDispensedMedications().isEmpty()) {
            container.getChildren().add(medicationSection("Dispensed Medications", Context.DISPENSED_MEDICATION,
                    document.getDispensedMedications(), "D"));
        }

        final ScrollPane scrollPane = new ScrollPane(container);
        scrollPane.setFitToWidth(true);
        scrollPane.getStyleClass().add("prescription-scroll");
        return scrollPane;
    }

    private static TitledPane section(String title, Context context, Map<String, String> attributes) {
        final TitledPane titledPane = new TitledPane(title, attributeGrid(context, attributes));
        titledPane.setCollapsible(false);
        titledPane.getStyleClass().add("prescription-section");
        return titledPane;
    }

    private static TitledPane medicationSection(String title, Context context, List<Map<String, String>> medications,
            String... titleAttributeCandidates) {
        final VBox medicationsBox = new VBox(8);
        int index = 1;
        for (Map<String, String> medication : medications) {
            final String label = titleFor(index, medication, titleAttributeCandidates);
            final TitledPane medicationPane = new TitledPane(label, attributeGrid(context, medication));
            medicationPane.getStyleClass().add("prescription-medication");
            medicationsBox.getChildren().add(medicationPane);
            index++;
        }
        final TitledPane titledPane = new TitledPane(title + " (" + medications.size() + ")", medicationsBox);
        titledPane.setCollapsible(false);
        titledPane.getStyleClass().add("prescription-section");
        return titledPane;
    }

    private static String titleFor(int index, Map<String, String> medication, String[] titleAttributeCandidates) {
        for (String candidate : titleAttributeCandidates) {
            final String value = medication.get(candidate);
            if (value != null && !value.isBlank()) {
                return "Medication " + index + " - " + value;
            }
        }
        return "Medication " + index;
    }

    private static GridPane attributeGrid(Context context, Map<String, String> attributes) {
        final GridPane grid = new GridPane();
        grid.getStyleClass().add("prescription-grid");
        grid.setHgap(12);
        grid.setVgap(4);

        final ColumnConstraints labelColumn = new ColumnConstraints();
        labelColumn.setMinWidth(220);
        final ColumnConstraints valueColumn = new ColumnConstraints();
        valueColumn.setHgrow(Priority.ALWAYS);
        grid.getColumnConstraints().addAll(labelColumn, valueColumn);

        int row = 0;
        for (Map.Entry<String, String> attribute : attributes.entrySet()) {
            final String label = PrescriptionFieldDictionary.getLabel(context, attribute.getKey());
            final String value = PrescriptionFieldDictionary.describeValue(context, attribute.getKey(), attribute.getValue());

            final Label labelNode = new Label(label);
            labelNode.getStyleClass().add("prescription-field-label");
            labelNode.setWrapText(true);

            final Label valueNode = new Label(value);
            valueNode.getStyleClass().add("prescription-field-value");
            valueNode.setWrapText(true);

            grid.addRow(row, labelNode, valueNode);
            row++;
        }

        return grid;
    }
}
