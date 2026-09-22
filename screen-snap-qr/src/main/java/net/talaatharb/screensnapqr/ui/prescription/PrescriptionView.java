package net.talaatharb.screensnapqr.ui.prescription;

import java.util.List;
import java.util.Map;

import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.control.TitledPane;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import net.talaatharb.screensnapqr.ui.prescription.PrescriptionFieldDictionary.Context;

/**
 * Builds a human-readable JavaFX visualization of a parsed
 * {@link PrescriptionDocument}, resolving raw two-letter attribute codes to
 * their meaning via {@link PrescriptionFieldDictionary}. The layout is
 * responsive (sections and fields grow/shrink with the containing window) and
 * field values are rendered as read-only, selectable/copyable text fields.
 */
public final class PrescriptionView {

    private static final double LABEL_COLUMN_PERCENT_WIDTH = 38;
    private static final double VALUE_COLUMN_PERCENT_WIDTH = 62;

    private PrescriptionView() {
    }

    public static Node build(PrescriptionDocument document) {
        final VBox container = new VBox(10);
        container.getStyleClass().add("prescription-view");
        container.setPadding(new Insets(10));
        container.setMaxWidth(Double.MAX_VALUE);
        container.setFillWidth(true);

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

        // Keep the container width in sync with the visible viewport so sections/fields
        // reflow as the window is resized instead of staying at their initial width.
        scrollPane.viewportBoundsProperty().addListener((observable, oldValue, bounds) -> {
            final double width = bounds.getWidth() - container.getInsets().getLeft() - container.getInsets().getRight();
            container.setPrefWidth(Math.max(0, width));
        });

        return scrollPane;
    }

    private static TitledPane section(String title, Context context, Map<String, String> attributes) {
        final GridPane grid = attributeGrid(context, attributes);
        final TitledPane titledPane = new TitledPane(title, grid);
        titledPane.setCollapsible(false);
        titledPane.setMaxWidth(Double.MAX_VALUE);
        titledPane.getStyleClass().add("prescription-section");
        return titledPane;
    }

    private static TitledPane medicationSection(String title, Context context, List<Map<String, String>> medications,
            String... titleAttributeCandidates) {
        final VBox medicationsBox = new VBox(8);
        medicationsBox.setMaxWidth(Double.MAX_VALUE);
        medicationsBox.setFillWidth(true);
        int index = 1;
        for (Map<String, String> medication : medications) {
            final String label = PrescriptionFormatting.medicationTitle(index, medication, titleAttributeCandidates);
            final TitledPane medicationPane = new TitledPane(label, attributeGrid(context, medication));
            medicationPane.setMaxWidth(Double.MAX_VALUE);
            medicationPane.getStyleClass().add("prescription-medication");
            medicationsBox.getChildren().add(medicationPane);
            index++;
        }
        final TitledPane titledPane = new TitledPane(title + " (" + medications.size() + ")", medicationsBox);
        titledPane.setCollapsible(false);
        titledPane.setMaxWidth(Double.MAX_VALUE);
        titledPane.getStyleClass().add("prescription-section");
        return titledPane;
    }

    private static GridPane attributeGrid(Context context, Map<String, String> attributes) {
        final GridPane grid = new GridPane();
        grid.getStyleClass().add("prescription-grid");
        grid.setHgap(12);
        grid.setVgap(4);
        grid.setMaxWidth(Double.MAX_VALUE);

        final ColumnConstraints labelColumn = new ColumnConstraints();
        labelColumn.setPercentWidth(LABEL_COLUMN_PERCENT_WIDTH);
        final ColumnConstraints valueColumn = new ColumnConstraints();
        valueColumn.setPercentWidth(VALUE_COLUMN_PERCENT_WIDTH);
        valueColumn.setHgrow(Priority.ALWAYS);
        grid.getColumnConstraints().addAll(labelColumn, valueColumn);

        int row = 0;
        for (Map.Entry<String, String> attribute : attributes.entrySet()) {
            final String label = PrescriptionFieldDictionary.getLabel(context, attribute.getKey());
            final String value = PrescriptionFieldDictionary.describeValue(context, attribute.getKey(), attribute.getValue());

            final Label labelNode = new Label(label);
            labelNode.getStyleClass().add("prescription-field-label");
            labelNode.setWrapText(true);
            labelNode.setMaxWidth(Double.MAX_VALUE);

            grid.addRow(row, labelNode, valueField(value));
            row++;
        }

        return grid;
    }

    /**
     * Renders a field value as a read-only {@link TextField} rather than a
     * {@link Label} so the text can be selected and copied (Ctrl+C) directly
     * from the view.
     */
    private static TextField valueField(String value) {
        final TextField valueNode = new TextField(value != null ? value : "");
        valueNode.setEditable(false);
        valueNode.getStyleClass().add("prescription-field-value");
        valueNode.setMaxWidth(Double.MAX_VALUE);
        GridPane.setHgrow(valueNode, Priority.ALWAYS);
        return valueNode;
    }
}
