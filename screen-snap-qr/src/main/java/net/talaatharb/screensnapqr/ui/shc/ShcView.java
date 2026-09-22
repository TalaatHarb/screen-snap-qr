package net.talaatharb.screensnapqr.ui.shc;

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

import net.talaatharb.screensnapqr.ui.shc.ShcDocument.ShcResource;
import net.talaatharb.screensnapqr.ui.shc.ShcFieldDictionary.Context;

/**
 * Builds a human-readable JavaFX visualization of a parsed {@link ShcDocument}
 * (SMART Health Card), resolving raw FHIR field codes to their meaning via
 * {@link ShcFieldDictionary}. The layout is responsive and field values are
 * rendered as read-only, selectable/copyable text fields.
 */
public final class ShcView {

    private static final double LABEL_COLUMN_PERCENT_WIDTH = 38;
    private static final double VALUE_COLUMN_PERCENT_WIDTH = 62;

    private ShcView() {
    }

    public static Node build(ShcDocument document) {
        final VBox container = new VBox(10);
        container.getStyleClass().add("shc-view");
        container.setPadding(new Insets(10));
        container.setMaxWidth(Double.MAX_VALUE);
        container.setFillWidth(true);

        final Label title = new Label("SMART Health Card");
        title.getStyleClass().add("shc-title");
        container.getChildren().add(title);

        container.getChildren().add(section("Header", Context.HEADER, document.getHeaderAttributes()));

        int index = 1;
        for (ShcResource resource : document.getResources()) {
            container.getChildren().add(resourceSection(index, resource));
            index++;
        }

        final ScrollPane scrollPane = new ScrollPane(container);
        scrollPane.setFitToWidth(true);
        scrollPane.getStyleClass().add("shc-scroll");

        scrollPane.viewportBoundsProperty().addListener((observable, oldValue, bounds) -> {
            final double width = bounds.getWidth() - container.getInsets().getLeft() - container.getInsets().getRight();
            container.setPrefWidth(Math.max(0, width));
        });

        return scrollPane;
    }

    private static TitledPane resourceSection(int index, ShcResource resource) {
        final Context context = contextFor(resource.getResourceType());
        final String title = ShcFormatting.resourceTitle(index, resource.getResourceType(), null);

        final Node body = resource.getAttributes().isEmpty() ? rawJsonView(resource.getRawJson())
                : attributeGrid(context, resource.getAttributes());

        final TitledPane titledPane = new TitledPane(title, body);
        titledPane.setCollapsible(false);
        titledPane.setMaxWidth(Double.MAX_VALUE);
        titledPane.getStyleClass().add("shc-section");
        return titledPane;
    }

    private static TitledPane section(String title, Context context, Map<String, String> attributes) {
        final GridPane grid = attributeGrid(context, attributes);
        final TitledPane titledPane = new TitledPane(title, grid);
        titledPane.setCollapsible(false);
        titledPane.setMaxWidth(Double.MAX_VALUE);
        titledPane.getStyleClass().add("shc-section");
        return titledPane;
    }

    private static GridPane attributeGrid(Context context, Map<String, String> attributes) {
        final GridPane grid = new GridPane();
        grid.getStyleClass().add("shc-grid");
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
            final String label = ShcFieldDictionary.getLabel(context, attribute.getKey());

            final Label labelNode = new Label(label);
            labelNode.getStyleClass().add("shc-field-label");
            labelNode.setWrapText(true);
            labelNode.setMaxWidth(Double.MAX_VALUE);

            grid.addRow(row, labelNode, valueField(attribute.getValue()));
            row++;
        }

        return grid;
    }

    private static Node rawJsonView(String rawJson) {
        final TextField valueNode = valueField(rawJson);
        valueNode.getStyleClass().add("shc-field-value");
        return valueNode;
    }

    /**
     * Renders a field value as a read-only {@link TextField} rather than a
     * {@link Label} so the text can be selected and copied (Ctrl+C) directly
     * from the view.
     */
    private static TextField valueField(String value) {
        final TextField valueNode = new TextField(value != null ? value : "");
        valueNode.setEditable(false);
        valueNode.getStyleClass().add("shc-field-value");
        valueNode.setMaxWidth(Double.MAX_VALUE);
        GridPane.setHgrow(valueNode, Priority.ALWAYS);
        return valueNode;
    }

    private static Context contextFor(String resourceType) {
        if (resourceType == null) {
            return Context.GENERIC_RESOURCE;
        }
        return switch (resourceType) {
            case "Patient" -> Context.PATIENT;
            case "Immunization" -> Context.IMMUNIZATION;
            case "Observation" -> Context.OBSERVATION;
            case "Condition" -> Context.CONDITION;
            default -> Context.GENERIC_RESOURCE;
        };
    }
}
