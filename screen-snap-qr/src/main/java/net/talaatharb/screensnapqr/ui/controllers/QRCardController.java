package net.talaatharb.screensnapqr.ui.controllers;

import java.util.Base64;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
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

    public void setQRResult(QRCodeResultDto result) {
        Platform.runLater(() -> {
            textLabel.setText(result.getText());
            formatLabel.setText(result.getFormat().toString());
            rawBytesLabel.setText(Base64.getEncoder().encodeToString(result.getRawBytes()));
        });
    }
}
