package net.talaatharb.screensnapqr.ui.controllers;

import java.util.Base64;
import java.util.List;

import javafx.application.Platform;
import javafx.embed.swing.SwingFXUtils;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.Tab;
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
    private TreeView<String> zipTreeView;

    @Getter(value = AccessLevel.PACKAGE)
    @Setter(value = AccessLevel.PACKAGE)
    @FXML
    private Tab zipTab;

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

        zipTab.setDisable(false);
        zipTreeView.setRoot(buildTreeItem(zipNode));
        zipTreeView.getRoot().setExpanded(true);
    }

    private static TreeItem<String> buildTreeItem(ZipNode node) {
        final TreeItem<String> treeItem = new TreeItem<>(node.getLabel());
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