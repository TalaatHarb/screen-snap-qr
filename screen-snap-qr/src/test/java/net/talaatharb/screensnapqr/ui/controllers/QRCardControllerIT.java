package net.talaatharb.screensnapqr.ui.controllers;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.testfx.framework.junit5.ApplicationTest;

import javafx.application.Platform;
import javafx.scene.control.Label;
import net.talaatharb.screensnapqr.constants.QRCodeFormat;
import net.talaatharb.screensnapqr.dtos.QRCodeResultDto;

@ExtendWith(MockitoExtension.class)
class QRCardControllerIT extends ApplicationTest {

    @InjectMocks
    QRCardController qrCardController;

    @BeforeEach
    void initializeController() {
        Platform.runLater(() -> {
            qrCardController.setTextLabel(new Label());
            qrCardController.setFormatLabel(new Label());
            qrCardController.setRawBytesLabel(new Label());
        });
    }

    @Test
    void testSetQRResult() {
        QRCodeResultDto result = new QRCodeResultDto("test", new byte[] {}, 0, QRCodeFormat.QR_CODE, 0);
        qrCardController.setQRResult(result);

        Platform.runLater(() -> Assertions.assertEquals(result.getText(), qrCardController.getTextLabel().getText()));
    }

}
