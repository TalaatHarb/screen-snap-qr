package net.talaatharb.screensnapqr.ui.controllers;

import static org.awaitility.Awaitility.await;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.testfx.framework.junit5.ApplicationTest;

import javafx.application.Platform;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Label;
import javafx.scene.control.Spinner;
import javafx.scene.layout.FlowPane;
import net.talaatharb.screensnapqr.constants.QRCodeFormat;
import net.talaatharb.screensnapqr.dtos.QRCodeResultDto;
import net.talaatharb.screensnapqr.facade.ScreenSnapQRFacade;

@ExtendWith(MockitoExtension.class)
class MainUiControllerIT extends ApplicationTest {

    @InjectMocks
    MainUiController uiController;

    @Mock
    ScreenSnapQRFacade screenSnapQRFacade;

    @BeforeEach
    void initializeController() {
        Platform.runLater(() -> {
            uiController.setDelaySpinner(new Spinner<>());
            uiController.setDelayLabel(new Label());
            uiController.setModeChoiceBox(new ChoiceBox<>());
            uiController.setQrCards(new FlowPane());
            uiController.initialize(null, null);
        });
    }

    @Test
    void testNewQRSnap() throws Exception {
        final int delay = 2;
        QRCodeResultDto result = new QRCodeResultDto("test", new byte[] {}, 0, QRCodeFormat.QR_CODE, 0);
        when(screenSnapQRFacade.getAllQRCodesFromScreen())
                .thenReturn(List.of(result));

        Platform.runLater(() -> uiController.getDelaySpinner().getValueFactory().setValue(delay));
        Platform.runLater(() -> uiController.newQRSnap());

        await()
                .atLeast(delay, TimeUnit.SECONDS)
                .atMost(delay + 1, TimeUnit.SECONDS)
                .untilAsserted(() -> verify(screenSnapQRFacade).getAllQRCodesFromScreen());
    }

}
