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
import net.talaatharb.screensnapqr.ui.content.ScannedContentViewModel;
import net.talaatharb.screensnapqr.ui.content.ZipNode;
import net.talaatharb.screensnapqr.ui.viewer.DocumentViewer;

public class QRCardController {

    private final ScannedContentAnalyzer scannedContentAnalyzer = new ScannedContentAnalyzer();

    private String lastCopyableText = "";

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

    void openDocumentViewer(ZipNode node) {
        if (node != null && !node.isDirectory()) {
            final String title = node.getPath() != null && !node.getPath().isBlank() ? node.getPath() : node.getLabel();
            DocumentViewer.show(title, node.getTextContent());
        }
    }

    public void setQRResult(QRCodeResultDto result) {
        final ScannedContentViewModel contentModel = scannedContentAnalyzer.analyze(result);
        Platform.runLater(() -> {
            formatLabel.setText(result.getFormat().toString());

            renderContent(contentModel.getTokens());
            contentTypeLabel.setText(contentModel.getContentType().getDisplayName());
            lastCopyableText = contentModel.getCopyableText();

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