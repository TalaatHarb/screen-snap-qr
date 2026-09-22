package net.talaatharb.screensnapqr.ui.dscsa;

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

/**
 * Builds a human-readable JavaFX visualization of a parsed {@link Gs1Document}
 * (US DSCSA GS1 DataMatrix package identifier), resolving raw AI codes to
 * their meaning via {@link Gs1FieldDictionary}. The layout is responsive and
 * field values are rendered as read-only, selectable/copyable text fields.
 */
public final class Gs1View {

    private static final double LABEL_COLUMN_PERCENT_WIDTH = 38;
    private static final double VALUE_COLUMN_PERCENT_WIDTH = 62;

    private Gs1View() {
    }

    public static Node build(Gs1Document document) {
        final VBox container = new VBox(10);
        container.getStyleClass().add("dscsa-view");
        container.setPadding(new Insets(10));
        container.setMaxWidth(Double.MAX_VALUE);
        container.setFillWidth(true);

        final Label title = new Label("US DSCSA Package Identifier (GS1 DataMatrix)");
        title.getStyleClass().add("dscsa-title");
        container.getChildren().add(title);

        container.getChildren().add(section("Package Identifiers", document.getElements()));

        final ScrollPane scrollPane = new ScrollPane(container);
        scrollPane.setFitToWidth(true);
        scrollPane.getStyleClass().add("dscsa-scroll");

        scrollPane.viewportBoundsProperty().addListener((observable, oldValue, bounds) -> {
            final double width = bounds.getWidth() - container.getInsets().getLeft() - container.getInsets().getRight();
            container.setPrefWidth(Math.max(0, width));
        });

        return scrollPane;
    }

    private static TitledPane section(String title, Map<String, String> elements) {
        final GridPane grid = attributeGrid(elements);
        final TitledPane titledPane = new TitledPane(title, grid);
        titledPane.setCollapsible(false);
        titledPane.setMaxWidth(Double.MAX_VALUE);
        titledPane.getStyleClass().add("dscsa-section");
        return titledPane;
    }

    private static GridPane attributeGrid(Map<String, String> elements) {
        final GridPane grid = new GridPane();
        grid.getStyleClass().add("dscsa-grid");
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
        for (Map.Entry<String, String> element : elements.entrySet()) {
            final String label = Gs1FieldDictionary.getLabel(element.getKey()) + " (AI " + element.getKey() + ")";

            final Label labelNode = new Label(label);
            labelNode.getStyleClass().add("dscsa-field-label");
            labelNode.setWrapText(true);
            labelNode.setMaxWidth(Double.MAX_VALUE);

            grid.addRow(row, labelNode, valueField(element.getValue()));
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
        valueNode.getStyleClass().add("dscsa-field-value");
        valueNode.setMaxWidth(Double.MAX_VALUE);
        GridPane.setHgrow(valueNode, Priority.ALWAYS);
        return valueNode;
    }
}
