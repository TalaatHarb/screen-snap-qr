package net.talaatharb.screensnapqr.ui.controllers;

import java.io.IOException;
import java.net.URL;
import java.util.Arrays;
import java.util.List;
import java.util.ResourceBundle;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Label;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory.IntegerSpinnerValueFactory;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.Pane;
import javafx.stage.Stage;
import javafx.util.StringConverter;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import net.talaatharb.screensnapqr.config.HelperBeans;
import net.talaatharb.screensnapqr.constants.ModeChoice;
import net.talaatharb.screensnapqr.dtos.QRCodeResultDto;
import net.talaatharb.screensnapqr.facade.ScreenSnapQRFacade;
import net.talaatharb.screensnapqr.ui.capture.CaptureBoundsProvider;
import net.talaatharb.screensnapqr.ui.capture.DefaultCaptureBoundsProvider;
import net.talaatharb.screensnapqr.ui.capture.SelectionOverlay;
import net.talaatharb.screensnapqr.ui.capture.ActiveWindowBoundsResolver;

@Slf4j
@RequiredArgsConstructor
public class MainUiController implements Initializable {

    private final ScreenSnapQRFacade screenSnapQRFacade;
    private final CaptureBoundsProvider captureBoundsProvider;

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

    @Getter(value = AccessLevel.PACKAGE)
    @Setter(value = AccessLevel.PACKAGE)
    @FXML
    private FlowPane qrCards;

    public MainUiController() {
        screenSnapQRFacade = HelperBeans.buildScreenSnapQRFacade();
        captureBoundsProvider = new DefaultCaptureBoundsProvider(new SelectionOverlay(), new ActiveWindowBoundsResolver());
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
                    List<QRCodeResultDto> result = captureWithMainWindowHidden(() -> {
                        var captureBounds = captureBoundsProvider.resolveBounds(mode);
                        return captureQRCodes(mode, captureBounds);
                    });
                    log.info(result.toString());
                    Platform.runLater(() -> {
                        qrCards.getChildren().clear();
                        result.forEach(this::loadQRCardResult);
                    });
                } catch (Exception e) {
                    log.error("Unable to fetch QR codes from snap due to: {}", e.getMessage());
                }
                Platform.runLater(() -> delayLabel.setVisible(false));
            }, "QR-Capture").start();
        }, "Sleep-Thread").start();

    }

    private void loadQRCardResult(QRCodeResultDto r) {
        var loader = new FXMLLoader(getClass().getResource("/net/talaatharb/screensnapqr/ui/QRCard.fxml"));
        try {
            Pane card = loader.load();
            QRCardController controller = loader.getController();
            controller.setQRResult(r);
            qrCards.getChildren().add(card);
        } catch (IOException e) {
            log.error(e.getMessage());
        }
    }

    private List<QRCodeResultDto> captureQRCodes(ModeChoice mode, java.awt.Rectangle captureBounds) throws Exception {
        if (mode == ModeChoice.SCREEN) {
            return screenSnapQRFacade.getAllQRCodesFromScreen();
        }
        if (mode == ModeChoice.SELECTION) {
            if (captureBounds == null) {
                return List.of();
            }
            Thread.sleep(150);
            return filterBySelectionBounds(screenSnapQRFacade.getAllQRCodesFromScreen(), captureBounds);
        }
        if (captureBounds == null) {
            return List.of();
        }
        return screenSnapQRFacade.getAllQRCodesFromScreen(captureBounds);
    }

    private List<QRCodeResultDto> filterBySelectionBounds(List<QRCodeResultDto> results, java.awt.Rectangle selectionBounds) {
        return results.stream()
                .filter(result -> hasPointInsideBounds(result, selectionBounds))
                .toList();
    }

    private boolean hasPointInsideBounds(QRCodeResultDto result, java.awt.Rectangle selectionBounds) {
        var points = result.getResultPoints();
        if (points == null || points.length == 0) {
            return false;
        }
        return Arrays.stream(points).anyMatch(
                p -> selectionBounds.contains(Math.round(p.getX()), Math.round(p.getY())));
    }

    private List<QRCodeResultDto> captureWithMainWindowHidden(Callable<List<QRCodeResultDto>> captureAction) throws Exception {
        Stage stage = getMainStage();
        if (stage == null) {
            return captureAction.call();
        }

        runOnFxThread(() -> {
            stage.setIconified(true);
            stage.toBack();
        });
        Thread.sleep(180);

        try {
            return captureAction.call();
        } finally {
            runOnFxThread(() -> {
                stage.setIconified(false);
                stage.show();
                stage.toFront();
                stage.requestFocus();
            });
        }
    }

    private Stage getMainStage() {
        if (delayLabel == null || delayLabel.getScene() == null || delayLabel.getScene().getWindow() == null) {
            return null;
        }
        return (Stage) delayLabel.getScene().getWindow();
    }

    private void runOnFxThread(Runnable runnable) throws Exception {
        if (Platform.isFxApplicationThread()) {
            runnable.run();
            return;
        }
        CompletableFuture<Void> future = new CompletableFuture<>();
        Platform.runLater(() -> {
            try {
                runnable.run();
                future.complete(null);
            } catch (Exception e) {
                future.completeExceptionally(e);
            }
        });
        try {
            future.get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw e;
        } catch (ExecutionException e) {
            Throwable cause = e.getCause();
            if (cause instanceof Exception ex) {
                throw ex;
            }
            throw new IllegalStateException("Failed to execute operation on FX thread.", cause);
        }
    }
}
