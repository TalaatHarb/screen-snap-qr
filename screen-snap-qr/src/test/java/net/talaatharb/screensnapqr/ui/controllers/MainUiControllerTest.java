package net.talaatharb.screensnapqr.ui.controllers;

import static org.awaitility.Awaitility.await;
import static org.mockito.Mockito.verify;

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
import net.talaatharb.screensnapqr.facade.ScreenSnapQRFacade;

@ExtendWith(MockitoExtension.class)
class MainUiControllerTest extends ApplicationTest {

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
            uiController.initialize(null, null);
        });
    }

    @Test
    void testNewQRSnap() {
        final int delay = 2;

        Platform.runLater(() -> uiController.getDelaySpinner().getValueFactory().setValue(delay));
        Platform.runLater(() -> uiController.newQRSnap());

        await()
                .atLeast(delay, TimeUnit.SECONDS)
                .atMost(delay + 1, TimeUnit.SECONDS)
                .untilAsserted(() -> verify(screenSnapQRFacade).getAllQRCodesFromScreen());
    }

}
