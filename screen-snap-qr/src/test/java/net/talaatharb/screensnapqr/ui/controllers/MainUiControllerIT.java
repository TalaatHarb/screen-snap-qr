package net.talaatharb.screensnapqr.ui.controllers;

import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.awt.Rectangle;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Assertions;
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
import net.talaatharb.screensnapqr.ui.capture.CaptureBoundsProvider;

@ExtendWith(MockitoExtension.class)
class MainUiControllerIT extends ApplicationTest {

    @InjectMocks
    MainUiController uiController;

    @Mock
    ScreenSnapQRFacade screenSnapQRFacade;

    @Mock
    CaptureBoundsProvider captureBoundsProvider;

    @Mock
    net.talaatharb.screensnapqr.ui.capture.FileChooserProvider fileChooserProvider;

    @BeforeEach
    void initializeController() {
        Platform.runLater(() -> {
            uiController.setRootPane(new javafx.scene.layout.AnchorPane());
            uiController.setDelaySpinner(new Spinner<>());
            uiController.setDelayLabel(new Label());
            uiController.setModeChoiceBox(new ChoiceBox<>());
            uiController.setQrCards(new FlowPane());
            uiController.initialize(null, null);
        });
        await().atMost(1, TimeUnit.SECONDS).untilAsserted(() -> Assertions.assertNotNull(uiController.getModeChoiceBox()));
    }

    @Test
    void testNewQRSnap() throws Exception {
        final int delay = 2;
        QRCodeResultDto result = new QRCodeResultDto("test", new byte[] {}, 0, QRCodeFormat.QR_CODE, 0);
        when(captureBoundsProvider.resolveBounds(any())).thenReturn(null);
        when(screenSnapQRFacade.getAllQRCodesFromScreen())
                .thenReturn(List.of(result));

        Platform.runLater(() -> uiController.getDelaySpinner().getValueFactory().setValue(delay));
        Platform.runLater(() -> uiController.newQRSnap());

        await()
                .atMost(delay + 8, TimeUnit.SECONDS)
                .untilAsserted(() -> verify(screenSnapQRFacade).getAllQRCodesFromScreen());
    }

    @Test
    void testNewQRSnapSelectionModeUsesBoundsCapture() throws Exception {
        final int delay = 0;
        Rectangle bounds = new Rectangle(20, 30, 200, 150);
        QRCodeResultDto result = new QRCodeResultDto("test", new byte[] {}, 0, QRCodeFormat.QR_CODE, 0);
        when(captureBoundsProvider.resolveBounds(any())).thenReturn(bounds);
        result.setResultPoints(new net.talaatharb.screensnapqr.dtos.QRResultPointDto[] {
                new net.talaatharb.screensnapqr.dtos.QRResultPointDto(40, 60) });
        when(screenSnapQRFacade.getAllQRCodesFromScreen()).thenReturn(List.of(result));

        Platform.runLater(() -> {
            uiController.getDelaySpinner().getValueFactory().setValue(delay);
            uiController.getModeChoiceBox().setValue(net.talaatharb.screensnapqr.constants.ModeChoice.SELECTION);
            uiController.newQRSnap();
        });

        await()
                .atMost(8, TimeUnit.SECONDS)
                .untilAsserted(() -> verify(screenSnapQRFacade).getAllQRCodesFromScreen());
        verify(screenSnapQRFacade, never()).getAllQRCodesFromScreen(bounds);
    }

    @Test
    void testNoArgConstructorBuildsRealDependencies() {
        // The no-arg constructor (used by the FXMLLoader in the real app, unlike the
        // all-args constructor Mockito's @InjectMocks picks) is otherwise never exercised.
        Assertions.assertDoesNotThrow(() -> new MainUiController());
    }

    @Test
    void testModeChoiceStringConverterConvertsBothWays() {
        final javafx.util.StringConverter<net.talaatharb.screensnapqr.constants.ModeChoice> converter = uiController
                .getModeChoiceBox().getConverter();

        Assertions.assertNull(converter.toString(null));
        Assertions.assertEquals("Selection", converter.toString(net.talaatharb.screensnapqr.constants.ModeChoice.SELECTION));
        Assertions.assertEquals(net.talaatharb.screensnapqr.constants.ModeChoice.WINDOW, converter.fromString("Focused Window"));
        // Unrecognized text falls back to SCREEN.
        Assertions.assertEquals(net.talaatharb.screensnapqr.constants.ModeChoice.SCREEN, converter.fromString("does-not-exist"));
    }

    @Test
    void testNewQRSnapSelectionModeWithNullBoundsSkipsCapture() throws Exception {
        when(captureBoundsProvider.resolveBounds(any())).thenReturn(null);

        Platform.runLater(() -> {
            uiController.getDelaySpinner().getValueFactory().setValue(0);
            uiController.getModeChoiceBox().setValue(net.talaatharb.screensnapqr.constants.ModeChoice.SELECTION);
            uiController.newQRSnap();
        });

        await()
                .atMost(8, TimeUnit.SECONDS)
                .untilAsserted(() -> verify(captureBoundsProvider).resolveBounds(net.talaatharb.screensnapqr.constants.ModeChoice.SELECTION));
        verify(screenSnapQRFacade, never()).getAllQRCodesFromScreen();
        verify(screenSnapQRFacade, never()).getAllQRCodesFromScreen(any());
    }

    @Test
    void testNewQRSnapWindowModeWithNullBoundsSkipsCapture() throws Exception {
        when(captureBoundsProvider.resolveBounds(any())).thenReturn(null);

        Platform.runLater(() -> {
            uiController.getDelaySpinner().getValueFactory().setValue(0);
            uiController.getModeChoiceBox().setValue(net.talaatharb.screensnapqr.constants.ModeChoice.WINDOW);
            uiController.newQRSnap();
        });

        await()
                .atMost(8, TimeUnit.SECONDS)
                .untilAsserted(() -> verify(captureBoundsProvider).resolveBounds(net.talaatharb.screensnapqr.constants.ModeChoice.WINDOW));
        verify(screenSnapQRFacade, never()).getAllQRCodesFromScreen();
        verify(screenSnapQRFacade, never()).getAllQRCodesFromScreen(any());
    }

    @Test
    void testNewQRSnapWindowModeWithBoundsCallsFacadeWithBounds() throws Exception {
        Rectangle bounds = new Rectangle(5, 5, 50, 50);
        when(captureBoundsProvider.resolveBounds(any())).thenReturn(bounds);
        when(screenSnapQRFacade.getAllQRCodesFromScreen(bounds)).thenReturn(List.of());

        Platform.runLater(() -> {
            uiController.getDelaySpinner().getValueFactory().setValue(0);
            uiController.getModeChoiceBox().setValue(net.talaatharb.screensnapqr.constants.ModeChoice.WINDOW);
            uiController.newQRSnap();
        });

        await()
                .atMost(8, TimeUnit.SECONDS)
                .untilAsserted(() -> verify(screenSnapQRFacade).getAllQRCodesFromScreen(bounds));
    }

    @Test
    void testNewQRSnapFiltersOutResultsWithoutResultPoints() throws Exception {
        Rectangle bounds = new Rectangle(20, 30, 200, 150);
        // No result points set (defaults to null), so the selection-bounds filter should
        // discard this result instead of matching it.
        QRCodeResultDto result = new QRCodeResultDto("test", new byte[] {}, 0, QRCodeFormat.QR_CODE, 0);
        when(captureBoundsProvider.resolveBounds(any())).thenReturn(bounds);
        when(screenSnapQRFacade.getAllQRCodesFromScreen()).thenReturn(List.of(result));

        Platform.runLater(() -> {
            uiController.getDelaySpinner().getValueFactory().setValue(0);
            uiController.getModeChoiceBox().setValue(net.talaatharb.screensnapqr.constants.ModeChoice.SELECTION);
            uiController.newQRSnap();
        });

        await()
                .atMost(8, TimeUnit.SECONDS)
                .untilAsserted(() -> verify(screenSnapQRFacade).getAllQRCodesFromScreen());
        await().atMost(2, TimeUnit.SECONDS)
                .untilAsserted(() -> Assertions.assertTrue(uiController.getQrCards().getChildren().isEmpty()));
    }

    @Test
    void testNewQRSnapLogsAndRecoversWhenFacadeThrows() throws Exception {
        when(captureBoundsProvider.resolveBounds(any())).thenReturn(null);
        when(screenSnapQRFacade.getAllQRCodesFromScreen()).thenThrow(new RuntimeException("capture failed"));

        Platform.runLater(() -> {
            uiController.getDelaySpinner().getValueFactory().setValue(0);
            uiController.getModeChoiceBox().setValue(net.talaatharb.screensnapqr.constants.ModeChoice.SCREEN);
            uiController.newQRSnap();
        });

        // The background thread swallows the exception (logging it) and still re-hides the delay label.
        await().atMost(8, TimeUnit.SECONDS)
                .untilAsserted(() -> Assertions.assertFalse(uiController.getDelayLabel().isVisible()));
    }

    @Test
    void testNewQRSnapHidesAndRestoresRealMainStage() throws Exception {
        when(captureBoundsProvider.resolveBounds(any())).thenReturn(null);
        when(screenSnapQRFacade.getAllQRCodesFromScreen()).thenReturn(List.of());

        final java.util.concurrent.atomic.AtomicReference<javafx.stage.Stage> stageRef = new java.util.concurrent.atomic.AtomicReference<>();
        Platform.runLater(() -> {
            final javafx.stage.Stage stage = new javafx.stage.Stage();
            stage.setScene(new javafx.scene.Scene(uiController.getDelayLabel(), 100, 100));
            stage.show();
            stageRef.set(stage);
        });
        await().atMost(2, TimeUnit.SECONDS).untilAsserted(() -> Assertions.assertNotNull(stageRef.get()));

        Platform.runLater(() -> {
            uiController.getDelaySpinner().getValueFactory().setValue(0);
            uiController.getModeChoiceBox().setValue(net.talaatharb.screensnapqr.constants.ModeChoice.SCREEN);
            uiController.newQRSnap();
        });

        await().atMost(8, TimeUnit.SECONDS)
                .untilAsserted(() -> verify(screenSnapQRFacade).getAllQRCodesFromScreen());
        // The real stage should end up shown/deiconified again once the capture completes.
        await().atMost(4, TimeUnit.SECONDS)
                .untilAsserted(() -> Assertions.assertFalse(stageRef.get().isIconified()));
        Assertions.assertTrue(stageRef.get().isShowing());

        Platform.runLater(() -> stageRef.get().close());
    }

    @Test
    void testImportImageWithFileSelected() throws Exception {
        java.io.File file = new java.io.File("sample.png");
        when(fileChooserProvider.showOpenDialog(any(), any(), any(), any())).thenReturn(file);
        QRCodeResultDto result = new QRCodeResultDto("image_qr", new byte[] {}, 0, QRCodeFormat.QR_CODE, 0);
        when(screenSnapQRFacade.getAllQRCodesFromImageFile(file)).thenReturn(List.of(result));

        Platform.runLater(() -> uiController.importImage());

        await().atMost(5, TimeUnit.SECONDS)
                .untilAsserted(() -> verify(screenSnapQRFacade).getAllQRCodesFromImageFile(file));
    }

    @Test
    void testImportImageWithNullFileDoesNothing() throws Exception {
        when(fileChooserProvider.showOpenDialog(any(), any(), any(), any())).thenReturn(null);

        Platform.runLater(() -> uiController.importImage());

        verify(screenSnapQRFacade, never()).getAllQRCodesFromImageFile(any());
    }

    @Test
    void testImportPdfWithFileSelected() throws Exception {
        java.io.File file = new java.io.File("sample.pdf");
        when(fileChooserProvider.showOpenDialog(any(), any(), any(), any())).thenReturn(file);
        QRCodeResultDto result = new QRCodeResultDto("pdf_qr", new byte[] {}, 0, QRCodeFormat.QR_CODE, 0);
        when(screenSnapQRFacade.getAllQRCodesFromPdfFile(file)).thenReturn(List.of(result));

        Platform.runLater(() -> uiController.importPdf());

        await().atMost(5, TimeUnit.SECONDS)
                .untilAsserted(() -> verify(screenSnapQRFacade).getAllQRCodesFromPdfFile(file));
    }

    @Test
    void testImportPdfWithNullFileDoesNothing() throws Exception {
        when(fileChooserProvider.showOpenDialog(any(), any(), any(), any())).thenReturn(null);

        Platform.runLater(() -> uiController.importPdf());

        verify(screenSnapQRFacade, never()).getAllQRCodesFromPdfFile(any());
    }

    @Test
    void testImportFromClipboard() throws Exception {
        QRCodeResultDto result = new QRCodeResultDto("clip_qr", new byte[] {}, 0, QRCodeFormat.QR_CODE, 0);
        when(screenSnapQRFacade.getAllQRCodesFromClipboard()).thenReturn(List.of(result));

        Platform.runLater(() -> uiController.importFromClipboard());

        await().atMost(5, TimeUnit.SECONDS)
                .untilAsserted(() -> verify(screenSnapQRFacade).getAllQRCodesFromClipboard());
    }

    @Test
    void testImportFromFileHandlesExceptionsGracefully() throws Exception {
        java.io.File file = new java.io.File("bad.png");
        when(screenSnapQRFacade.getAllQRCodesFromImageFile(file)).thenThrow(new RuntimeException("read failed"));

        Platform.runLater(() -> uiController.importFromFile(file, false));

        await().atMost(5, TimeUnit.SECONDS)
                .untilAsserted(() -> verify(screenSnapQRFacade).getAllQRCodesFromImageFile(file));
    }

    @Test
    void testImportFromClipboardHandlesExceptionsGracefully() throws Exception {
        when(screenSnapQRFacade.getAllQRCodesFromClipboard()).thenThrow(new RuntimeException("clip failed"));

        Platform.runLater(() -> uiController.importFromClipboard());

        await().atMost(5, TimeUnit.SECONDS)
                .untilAsserted(() -> verify(screenSnapQRFacade).getAllQRCodesFromClipboard());
    }

    @Test
    void testSetupDragAndDropNullSafe() {
        Assertions.assertDoesNotThrow(() -> uiController.setupDragAndDrop(null));
    }
}
