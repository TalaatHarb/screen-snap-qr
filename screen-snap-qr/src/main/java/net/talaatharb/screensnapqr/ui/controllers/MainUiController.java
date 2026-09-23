package net.talaatharb.screensnapqr.ui.controllers;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Arrays;
import java.util.List;
import java.util.ResourceBundle;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import javafx.application.Platform;
import javafx.embed.swing.SwingFXUtils;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Label;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory.IntegerSpinnerValueFactory;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.Pane;
import javafx.stage.FileChooser.ExtensionFilter;
import javafx.stage.Stage;
import javafx.stage.Window;
import javafx.util.StringConverter;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import net.talaatharb.screensnapqr.config.HelperBeans;
import net.talaatharb.screensnapqr.constants.ModeChoice;
import net.talaatharb.screensnapqr.dtos.QRCodeResultDto;
import net.talaatharb.screensnapqr.facade.ScreenSnapQRFacade;
import net.talaatharb.screensnapqr.ui.capture.ActiveWindowBoundsResolver;
import net.talaatharb.screensnapqr.ui.capture.CaptureBoundsProvider;
import net.talaatharb.screensnapqr.ui.capture.DefaultCaptureBoundsProvider;
import net.talaatharb.screensnapqr.ui.capture.DefaultFileChooserProvider;
import net.talaatharb.screensnapqr.ui.capture.FileChooserProvider;
import net.talaatharb.screensnapqr.ui.capture.SelectionOverlay;

@Slf4j
public class MainUiController implements Initializable {

    private final ScreenSnapQRFacade screenSnapQRFacade;
    private final CaptureBoundsProvider captureBoundsProvider;
    private final FileChooserProvider fileChooserProvider;

    @Getter(value = AccessLevel.PACKAGE)
    @Setter(value = AccessLevel.PACKAGE)
    @FXML
    private AnchorPane rootPane;

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

    @Getter(value = AccessLevel.PACKAGE)
    @Setter(value = AccessLevel.PACKAGE)
    @FXML
    private Button importImageButton;

    @Getter(value = AccessLevel.PACKAGE)
    @Setter(value = AccessLevel.PACKAGE)
    @FXML
    private Button importPdfButton;

    @Getter(value = AccessLevel.PACKAGE)
    @Setter(value = AccessLevel.PACKAGE)
    @FXML
    private Button clipboardButton;

    public MainUiController() {
        this(HelperBeans.buildScreenSnapQRFacade(),
                new DefaultCaptureBoundsProvider(new SelectionOverlay(), new ActiveWindowBoundsResolver()),
                new DefaultFileChooserProvider());
    }

    public MainUiController(ScreenSnapQRFacade screenSnapQRFacade, CaptureBoundsProvider captureBoundsProvider) {
        this(screenSnapQRFacade, captureBoundsProvider, new DefaultFileChooserProvider());
    }

    public MainUiController(ScreenSnapQRFacade screenSnapQRFacade, CaptureBoundsProvider captureBoundsProvider,
            FileChooserProvider fileChooserProvider) {
        this.screenSnapQRFacade = screenSnapQRFacade;
        this.captureBoundsProvider = captureBoundsProvider;
        this.fileChooserProvider = fileChooserProvider != null ? fileChooserProvider : new DefaultFileChooserProvider();
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        log.info("Initializing UI application Main window controller...");

        if (delaySpinner != null) {
            delaySpinner.setValueFactory(new IntegerSpinnerValueFactory(0, 10));
        }

        if (modeChoiceBox != null) {
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

        if (rootPane != null) {
            setupDragAndDrop(rootPane);
            rootPane.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
                if (event.isControlDown() && event.getCode() == KeyCode.V) {
                    importFromClipboard();
                    event.consume();
                }
            });
        } else if (qrCards != null) {
            setupDragAndDrop(qrCards);
        }
    }

    @FXML
    void newQRSnap() {
        final Integer delay = delaySpinner != null ? delaySpinner.getValue() : 0;
        final ModeChoice mode = modeChoiceBox != null ? modeChoiceBox.getValue() : ModeChoice.SCREEN;

        new Thread(() -> {
            Platform.runLater(() -> {
                if (delayLabel != null) {
                    delayLabel.setVisible(true);
                    delayLabel.setText(delay + "");
                }
            });
            for (int i = 0; i < delay; i++) {
                try {
                    final var remaining = delay - i;
                    Platform.runLater(() -> {
                        if (delayLabel != null) {
                            delayLabel.setText(remaining + "");
                        }
                    });
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
                        if (qrCards != null) {
                            qrCards.getChildren().clear();
                            result.forEach(this::loadQRCardResult);
                        }
                    });
                } catch (Exception e) {
                    log.error("Unable to fetch QR codes from snap due to: {}", e.getMessage());
                }
                Platform.runLater(() -> {
                    if (delayLabel != null) {
                        delayLabel.setVisible(false);
                    }
                });
            }, "QR-Capture").start();
        }, "Sleep-Thread").start();

    }

    @FXML
    void importImage() {
        Window window = getWindow();
        File file = fileChooserProvider.showOpenDialog(window, "Open Image File",
                new ExtensionFilter("Image Files (*.png, *.jpg, *.jpeg, *.bmp, *.gif, *.webp)",
                        "*.png", "*.jpg", "*.jpeg", "*.bmp", "*.gif", "*.webp",
                        "*.PNG", "*.JPG", "*.JPEG", "*.BMP", "*.GIF", "*.WEBP"),
                new ExtensionFilter("All Files (*.*)", "*.*"));
        if (file != null) {
            importFromFile(file, false);
        }
    }

    @FXML
    void importPdf() {
        Window window = getWindow();
        File file = fileChooserProvider.showOpenDialog(window, "Open PDF File",
                new ExtensionFilter("PDF Files (*.pdf)", "*.pdf", "*.PDF"),
                new ExtensionFilter("All Files (*.*)", "*.*"));
        if (file != null) {
            importFromFile(file, true);
        }
    }

    @FXML
    void importFromClipboard() {
        new Thread(() -> {
            try {
                List<QRCodeResultDto> results = screenSnapQRFacade.getAllQRCodesFromClipboard();
                log.info("Clipboard import found {} code(s)", results.size());
                Platform.runLater(() -> {
                    if (qrCards != null) {
                        qrCards.getChildren().clear();
                        results.forEach(this::loadQRCardResult);
                    }
                });
            } catch (Exception e) {
                log.error("Unable to import QR codes from clipboard: {}", e.getMessage());
            }
        }, "QR-Clipboard-Import").start();
    }

    void importFromFile(File file, boolean isPdf) {
        new Thread(() -> {
            try {
                List<QRCodeResultDto> results = isPdf
                        ? screenSnapQRFacade.getAllQRCodesFromPdfFile(file)
                        : screenSnapQRFacade.getAllQRCodesFromImageFile(file);
                log.info("File import from {} found {} code(s)", file.getName(), results.size());
                Platform.runLater(() -> {
                    if (qrCards != null) {
                        qrCards.getChildren().clear();
                        results.forEach(this::loadQRCardResult);
                    }
                });
            } catch (Exception e) {
                log.error("Unable to import QR codes from file {}: {}", file.getName(), e.getMessage());
            }
        }, "QR-File-Import").start();
    }

    void setupDragAndDrop(Pane targetPane) {
        if (targetPane == null) {
            return;
        }
        targetPane.setOnDragOver(event -> {
            if (event.getDragboard().hasFiles() || event.getDragboard().hasImage()) {
                event.acceptTransferModes(TransferMode.COPY);
            }
            event.consume();
        });

        targetPane.setOnDragDropped(event -> {
            var db = event.getDragboard();
            boolean success = false;
            if (db.hasFiles()) {
                List<File> files = db.getFiles();
                if (files != null && !files.isEmpty()) {
                    File first = files.getFirst();
                    boolean isPdf = first.getName().toLowerCase().endsWith(".pdf");
                    importFromFile(first, isPdf);
                    success = true;
                }
            } else if (db.hasImage()) {
                var fxImg = db.getImage();
                if (fxImg != null) {
                    BufferedImage bImg = SwingFXUtils.fromFXImage(fxImg, null);
                    new Thread(() -> {
                        try {
                            List<QRCodeResultDto> results = screenSnapQRFacade.getAllQRCodesFromImage(bImg);
                            Platform.runLater(() -> {
                                if (qrCards != null) {
                                    qrCards.getChildren().clear();
                                    results.forEach(this::loadQRCardResult);
                                }
                            });
                        } catch (Exception e) {
                            log.error("Unable to import dropped image: {}", e.getMessage());
                        }
                    }, "QR-Drop-Import").start();
                    success = true;
                }
            }
            event.setDropCompleted(success);
            event.consume();
        });
    }

    private Window getWindow() {
        if (rootPane != null && rootPane.getScene() != null) {
            return rootPane.getScene().getWindow();
        }
        if (delayLabel != null && delayLabel.getScene() != null) {
            return delayLabel.getScene().getWindow();
        }
        return null;
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
