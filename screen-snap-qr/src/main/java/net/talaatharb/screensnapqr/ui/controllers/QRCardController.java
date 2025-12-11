package net.talaatharb.screensnapqr.ui.controllers;

import java.util.Base64;

import javafx.application.Platform;
import javafx.embed.swing.SwingFXUtils;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import net.talaatharb.screensnapqr.dtos.QRCodeResultDto;

@RequiredArgsConstructor
public class QRCardController {

    @Getter(value = AccessLevel.PACKAGE)
    @Setter(value = AccessLevel.PACKAGE)
    @FXML
    private Label textLabel;

    @Getter(value = AccessLevel.PACKAGE)
    @Setter(value = AccessLevel.PACKAGE)
    @FXML
    private Label formatLabel;

    @Getter(value = AccessLevel.PACKAGE)
    @Setter(value = AccessLevel.PACKAGE)
    @FXML
    private Label rawBytesLabel;

    @Getter(value = AccessLevel.PACKAGE)
    @Setter(value = AccessLevel.PACKAGE)
    @FXML
    private ImageView qrCodeView;

    public void setQRResult(QRCodeResultDto result) {
        Platform.runLater(() -> {
            textLabel.setText(result.getText());
            formatLabel.setText(result.getFormat().toString());
            
            byte[] rawBytes = result.getRawBytes();
            if (rawBytes != null) {
                rawBytesLabel.setText(Base64.getEncoder().encodeToString(rawBytes));
            }
            
            var qrCodeImage = result.getQrCodeImage();
            if (qrCodeImage != null) {
                qrCodeView.setImage(SwingFXUtils.toFXImage(qrCodeImage, null));
            }
        });
    }

    @FXML
    public void copyText() {
        String text = textLabel.getText();
        if (text != null && !text.isEmpty()) {
            ClipboardContent content = new ClipboardContent();
            content.putString(text);
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