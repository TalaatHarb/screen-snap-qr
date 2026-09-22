package net.talaatharb.screensnapqr.ui.controllers;

import java.util.Base64;
import java.util.List;

import javafx.application.Platform;
import javafx.embed.swing.SwingFXUtils;
import javafx.fxml.FXML;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.Tab;
import javafx.scene.control.TreeCell;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import javafx.scene.image.ImageView;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import net.talaatharb.screensnapqr.dtos.QRCodeResultDto;
import net.talaatharb.screensnapqr.ui.content.ContentToken;
import net.talaatharb.screensnapqr.ui.content.ScannedContentAnalyzer;
import net.talaatharb.screensnapqr.ui.content.ScannedContentType;
import net.talaatharb.screensnapqr.ui.content.ScannedContentViewModel;
import net.talaatharb.screensnapqr.ui.content.ZipNode;
import net.talaatharb.screensnapqr.ui.einvoice.InvoiceParser;
import net.talaatharb.screensnapqr.ui.prescription.PrescriptionParser;
import net.talaatharb.screensnapqr.ui.viewer.DocumentViewer;
import net.talaatharb.screensnapqr.ui.viewer.InvoiceViewer;
import net.talaatharb.screensnapqr.ui.viewer.PrescriptionViewer;

public class QRCardController {

    private final ScannedContentAnalyzer scannedContentAnalyzer = new ScannedContentAnalyzer();

    private String lastCopyableText = "";

    private ScannedContentType lastContentType = ScannedContentType.TEXT;

    @Getter(value = AccessLevel.PACKAGE)
    @Setter(value = AccessLevel.PACKAGE)
    @FXML
    private Label formatLabel;

    @Getter(value = AccessLevel.PACKAGE)
    @Setter(value = AccessLevel.PACKAGE)
    @FXML
    private Label contentTypeLabel;

    @Getter(value = AccessLevel.PACKAGE)
    @Setter(value = AccessLevel.PACKAGE)
    @FXML
    private Label rawBytesLabel;

    @Getter(value = AccessLevel.PACKAGE)
    @Setter(value = AccessLevel.PACKAGE)
    @FXML
    private ImageView qrCodeView;

    @Getter(value = AccessLevel.PACKAGE)
    @Setter(value = AccessLevel.PACKAGE)
    @FXML
    private TextFlow contentFlow;

    @Getter(value = AccessLevel.PACKAGE)
    @Setter(value = AccessLevel.PACKAGE)
    @FXML
    private TreeView<ZipNode> zipTreeView;

    @Getter(value = AccessLevel.PACKAGE)
    @Setter(value = AccessLevel.PACKAGE)
    @FXML
    private Tab zipTab;

    @FXML
    public void initialize() {
        setupZipTreeView();
        setupContentContextMenu();
    }

    private void setupZipTreeView() {
        if (zipTreeView == null) {
            return;
        }

        zipTreeView.setCellFactory(tv -> new TreeCell<>() {
            @Override
            protected void updateItem(ZipNode item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                    setContextMenu(null);
                } else {
                    setText(item.getLabel());
                    if (!item.isDirectory()) {
                        final ContextMenu contextMenu = new ContextMenu();
                        final MenuItem viewItem = new MenuItem("View content");
                        viewItem.setOnAction(event -> openDocumentViewer(item));
                        contextMenu.getItems().add(viewItem);

                        if (PrescriptionParser.tryParse(item.getTextContent()).isPresent()) {
                            final MenuItem viewPrescriptionItem = new MenuItem("View prescription");
                            viewPrescriptionItem.setOnAction(event -> openPrescriptionViewer(item));
                            contextMenu.getItems().add(viewPrescriptionItem);
                        }

                        if (InvoiceParser.tryParse(item.getTextContent()).isPresent()) {
                            final MenuItem viewInvoiceItem = new MenuItem("View e-invoice");
                            viewInvoiceItem.setOnAction(event -> openInvoiceViewer(item));
                            contextMenu.getItems().add(viewInvoiceItem);
                        }

                        setContextMenu(contextMenu);
                    } else {
                        setContextMenu(null);
                    }
                }
            }
        });

        zipTreeView.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2 && zipTreeView.getSelectionModel().getSelectedItem() != null) {
                final TreeItem<ZipNode> selectedItem = zipTreeView.getSelectionModel().getSelectedItem();
                if (selectedItem != null && selectedItem.getValue() != null && !selectedItem.getValue().isDirectory()) {
                    openDocumentViewer(selectedItem.getValue());
                }
            }
        });
    }

    /**
     * Attaches a right-click context menu directly to the "Rendered" content tab so
     * that a scanned code whose payload is plain (non-ZIP-wrapped) prescription/invoice
     * XML can still be opened in the dedicated viewer, not only ZIP archive entries.
     */
    private void setupContentContextMenu() {
        if (contentFlow == null) {
            return;
        }

        contentFlow.setOnContextMenuRequested(event -> {
            final ContextMenu contextMenu = new ContextMenu();
            final MenuItem viewItem = new MenuItem("View content");
            viewItem.setOnAction(e -> openRenderedDocumentViewer());
            contextMenu.getItems().add(viewItem);

            if (PrescriptionParser.tryParse(lastCopyableText).isPresent()) {
                final MenuItem viewPrescriptionItem = new MenuItem("View prescription");
                viewPrescriptionItem.setOnAction(e -> openRenderedPrescriptionViewer());
                contextMenu.getItems().add(viewPrescriptionItem);
            }

            if (InvoiceParser.tryParse(lastCopyableText).isPresent()) {
                final MenuItem viewInvoiceItem = new MenuItem("View e-invoice");
                viewInvoiceItem.setOnAction(e -> openRenderedInvoiceViewer());
                contextMenu.getItems().add(viewInvoiceItem);
            }

            contextMenu.show(contentFlow, event.getScreenX(), event.getScreenY());
        });
    }

    void openRenderedDocumentViewer() {
        if (lastCopyableText != null && !lastCopyableText.isEmpty()) {
            DocumentViewer.show("Scanned content" + syntheticExtensionFor(lastContentType), lastCopyableText);
        }
    }

    /**
     * {@link net.talaatharb.screensnapqr.ui.viewer.SyntaxHighlighterRegistry} selects a
     * highlighter by file extension, but rendered (non-ZIP) scanned content has no real
     * file name. Synthesize one from the detected {@link ScannedContentType} so JSON/XML
     * content opened via "View content" from the Rendered tab is still highlighted.
     */
    static String syntheticExtensionFor(ScannedContentType contentType) {
        return switch (contentType) {
            case JSON -> ".json";
            case XML -> ".xml";
            default -> ".txt";
        };
    }

    void openRenderedPrescriptionViewer() {
        PrescriptionParser.tryParse(lastCopyableText)
                .ifPresent(document -> PrescriptionViewer.show("Scanned content", document));
    }

    void openRenderedInvoiceViewer() {
        InvoiceParser.tryParse(lastCopyableText).ifPresent(document -> InvoiceViewer.show("Scanned content", document));
    }

    void openDocumentViewer(ZipNode node) {
        if (node != null && !node.isDirectory()) {
            final String title = node.getPath() != null && !node.getPath().isBlank() ? node.getPath() : node.getLabel();
            DocumentViewer.show(title, node.getTextContent());
        }
    }

    void openPrescriptionViewer(ZipNode node) {
        if (node != null && !node.isDirectory()) {
            PrescriptionParser.tryParse(node.getTextContent()).ifPresent(document -> {
                final String title = node.getPath() != null && !node.getPath().isBlank() ? node.getPath() : node.getLabel();
                PrescriptionViewer.show(title, document);
            });
        }
    }

    void openInvoiceViewer(ZipNode node) {
        if (node != null && !node.isDirectory()) {
            InvoiceParser.tryParse(node.getTextContent()).ifPresent(document -> {
                final String title = node.getPath() != null && !node.getPath().isBlank() ? node.getPath() : node.getLabel();
                InvoiceViewer.show(title, document);
            });
        }
    }

    public void setQRResult(QRCodeResultDto result) {
        final ScannedContentViewModel contentModel = scannedContentAnalyzer.analyze(result);
        Platform.runLater(() -> {
            formatLabel.setText(result.getFormat().toString());

            renderContent(contentModel.getTokens());
            contentTypeLabel.setText(contentModel.getContentType().getDisplayName());
            lastCopyableText = contentModel.getCopyableText();
            lastContentType = contentModel.getContentType();

            final byte[] rawBytes = result.getRawBytes();
            rawBytesLabel.setText(rawBytes == null ? "" : Base64.getEncoder().encodeToString(rawBytes));
            renderZipTree(contentModel.getZipNode());

            var qrCodeImage = result.getQrCodeImage();
            if (qrCodeImage != null) {
                qrCodeView.setImage(SwingFXUtils.toFXImage(qrCodeImage, null));
            }
        });
    }

    private void renderContent(List<ContentToken> tokens) {
        contentFlow.getChildren().clear();

        for (var token : tokens) {
            final Text node = new Text(token.value());
            if (token.styleClass() != null && !token.styleClass().isBlank()) {
                node.getStyleClass().add(token.styleClass());
            }
            contentFlow.getChildren().add(node);
        }
    }

    private void renderZipTree(ZipNode zipNode) {
        if (zipNode == null) {
            zipTab.setDisable(true);
            zipTreeView.setRoot(null);
            return;
        }

        setupZipTreeView();
        zipTab.setDisable(false);
        zipTreeView.setRoot(buildTreeItem(zipNode));
        zipTreeView.getRoot().setExpanded(true);
    }

    private static TreeItem<ZipNode> buildTreeItem(ZipNode node) {
        final TreeItem<ZipNode> treeItem = new TreeItem<>(node);
        node.getChildren().stream().map(QRCardController::buildTreeItem).forEach(treeItem.getChildren()::add);
        return treeItem;
    }

    @FXML
    public void copyText() {
        if (lastCopyableText != null && !lastCopyableText.isEmpty()) {
            ClipboardContent content = new ClipboardContent();
            content.putString(lastCopyableText);
            Clipboard.getSystemClipboard().setContent(content);
        }
    }

    @FXML
    public void copyRawBytes() {
        String rawBytes = rawBytesLabel.getText();
        if (rawBytes != null && !rawBytes.isEmpty()) {
            ClipboardContent content = new ClipboardContent();
            content.putString(rawBytes);
            Clipboard.getSystemClipboard().setContent(content);
        }
    }
}