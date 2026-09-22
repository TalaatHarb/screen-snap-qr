package net.talaatharb.screensnapqr.ui.einvoice;

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

import net.talaatharb.screensnapqr.ui.einvoice.InvoiceFieldDictionary.Context;

/**
 * Builds a human-readable JavaFX visualization of a parsed
 * {@link InvoiceDocument}, resolving raw UBL element paths to their meaning
 * via {@link InvoiceFieldDictionary}. The layout is responsive (sections and
 * fields grow/shrink with the containing window) and field values are
 * rendered as read-only, selectable/copyable text fields.
 */
public final class InvoiceView {

    private static final double LABEL_COLUMN_PERCENT_WIDTH = 38;
    private static final double VALUE_COLUMN_PERCENT_WIDTH = 62;

    private InvoiceView() {
    }

    public static Node build(InvoiceDocument document) {
        final VBox container = new VBox(10);
        container.getStyleClass().add("invoice-view");
        container.setPadding(new Insets(10));
        container.setMaxWidth(Double.MAX_VALUE);
        container.setFillWidth(true);

        final String kind = document.isCreditNote() ? "UBL Credit Note (<CreditNote>)" : "UBL Invoice (<Invoice>)";
        final Label title = new Label(kind);
        title.getStyleClass().add("invoice-title");
        container.getChildren().add(title);

        container.getChildren().add(section("Header", Context.HEADER, document.getHeaderFields()));

        if (document.getSupplierParty() != null) {
            container.getChildren().add(section("Seller (Supplier)", Context.SUPPLIER_PARTY, document.getSupplierParty()));
        }

        if (document.getCustomerParty() != null) {
            container.getChildren().add(section("Buyer (Customer)", Context.CUSTOMER_PARTY, document.getCustomerParty()));
        }

        if (document.getPaymentMeans() != null) {
            container.getChildren().add(section("Payment Means", Context.PAYMENT_MEANS, document.getPaymentMeans()));
        }

        if (document.getTaxTotal() != null) {
            container.getChildren().add(section("Tax Total", Context.TAX_TOTAL, document.getTaxTotal()));
        }

        if (document.getMonetaryTotal() != null) {
            container.getChildren().add(section("Monetary Totals", Context.MONETARY_TOTAL, document.getMonetaryTotal()));
        }

        if (!document.getLines().isEmpty()) {
            container.getChildren().add(lineSection(document.isCreditNote() ? "Credit Note Lines" : "Invoice Lines",
                    document.getLines()));
        }

        final ScrollPane scrollPane = new ScrollPane(container);
        scrollPane.setFitToWidth(true);
        scrollPane.getStyleClass().add("invoice-scroll");

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
        titledPane.getStyleClass().add("invoice-section");
        return titledPane;
    }

    private static TitledPane lineSection(String title, List<Map<String, String>> lines) {
        final VBox linesBox = new VBox(8);
        linesBox.setMaxWidth(Double.MAX_VALUE);
        linesBox.setFillWidth(true);
        int index = 1;
        for (Map<String, String> line : lines) {
            final String label = InvoiceFormatting.lineTitle(index, line, "Item/Name");
            final TitledPane linePane = new TitledPane(label, attributeGrid(Context.INVOICE_LINE, line));
            linePane.setMaxWidth(Double.MAX_VALUE);
            linePane.getStyleClass().add("invoice-line");
            linesBox.getChildren().add(linePane);
            index++;
        }
        final TitledPane titledPane = new TitledPane(title + " (" + lines.size() + ")", linesBox);
        titledPane.setCollapsible(false);
        titledPane.setMaxWidth(Double.MAX_VALUE);
        titledPane.getStyleClass().add("invoice-section");
        return titledPane;
    }

    private static GridPane attributeGrid(Context context, Map<String, String> attributes) {
        final GridPane grid = new GridPane();
        grid.getStyleClass().add("invoice-grid");
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
            final String label = InvoiceFieldDictionary.getLabel(context, attribute.getKey());
            final String value = InvoiceFieldDictionary.describeValue(context, attribute.getKey(), attribute.getValue());

            final Label labelNode = new Label(label);
            labelNode.getStyleClass().add("invoice-field-label");
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
        valueNode.getStyleClass().add("invoice-field-value");
        valueNode.setMaxWidth(Double.MAX_VALUE);
        GridPane.setHgrow(valueNode, Priority.ALWAYS);
        return valueNode;
    }
}
