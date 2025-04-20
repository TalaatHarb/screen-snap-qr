package net.talaatharb.screensnapqr.ui.controllers;

import java.net.URL;
import java.util.Arrays;
import java.util.ResourceBundle;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Label;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory.IntegerSpinnerValueFactory;
import javafx.util.StringConverter;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import net.talaatharb.screensnapqr.config.HelperBeans;
import net.talaatharb.screensnapqr.constants.ModeChoice;
import net.talaatharb.screensnapqr.facade.ScreenSnapQRFacade;

@Slf4j
@RequiredArgsConstructor
public class MainUiController implements Initializable {

    private final ScreenSnapQRFacade screenSnapQRFacade;

    @Getter(value = AccessLevel.PACKAGE)
    @Setter(value = AccessLevel.PACKAGE)
    @FXML
    private ChoiceBox<ModeChoice> modeChoiceBox;

    @Getter(value = AccessLevel.PACKAGE)
    @Setter(value = AccessLevel.PACKAGE)
    @FXML
    private Spinner<Integer> delaySpinner;

    @Getter(value = AccessLevel.PACKAGE)
    @Setter(value = AccessLevel.PACKAGE)
    @FXML
    private Label delayLabel;

    public MainUiController() {
        screenSnapQRFacade = HelperBeans.buildScreenSnapQRFacade();
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        log.info("Initializing UI application Main window controller...");

        delaySpinner.setValueFactory(new IntegerSpinnerValueFactory(0, 10));

        modeChoiceBox.getItems().addAll(ModeChoice.values());
        modeChoiceBox.setConverter(new StringConverter<>() {
            @Override
            public String toString(ModeChoice modeChoice) {
                return modeChoice == null ? null : modeChoice.getText();
            }

            @Override
            public ModeChoice fromString(String string) {
                return Arrays.asList(ModeChoice.values()).stream().filter(c -> c.getText().equals(string)).findFirst()
                        .orElse(ModeChoice.SCREEN);
            }
        });
        modeChoiceBox.setValue(ModeChoice.SCREEN);
    }

    @FXML
    void newQRSnap() {
        final Integer delay = delaySpinner.getValue();
        final ModeChoice mode = modeChoiceBox.getValue();

        new Thread(() -> {
            Platform.runLater(() -> {
                delayLabel.setVisible(true);
                delayLabel.setText(delay + "");
            });
            for (int i = 0; i < delay; i++) {
                try {
                    final var remaining = delay - i;
                    Platform.runLater(() -> delayLabel.setText(remaining + ""));
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    log.warn("Thread interrupted, {}", e.getMessage());
                    Thread.currentThread().interrupt();
                }
            }

            new Thread(() -> {
                try {
                    switch (mode) {
                        case SCREEN:
                        default:
                            var result = screenSnapQRFacade.getAllQRCodesFromScreen();
                            log.info(result.toString());
                    }
                } catch (Exception e) {
                    log.error("Unable to fetch QR codes from snap due to: {}", e.getMessage());
                }
                Platform.runLater(() -> delayLabel.setVisible(false));
            }, "QR-Capture").start();
        }, "Sleep-Thread").start();

    }
}
