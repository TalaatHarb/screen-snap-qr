package net.talaatharb.screensnapqr.ui.controllers;

import static org.awaitility.Awaitility.await;

import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.testfx.framework.junit5.ApplicationTest;

import javafx.application.Platform;
import javafx.scene.control.Label;
import javafx.scene.control.Tab;
import javafx.scene.control.TreeView;
import javafx.scene.text.TextFlow;
import net.talaatharb.screensnapqr.constants.QRCodeFormat;
import net.talaatharb.screensnapqr.dtos.QRCodeResultDto;

@ExtendWith(MockitoExtension.class)
class QRCardControllerIT extends ApplicationTest {

    @InjectMocks
    QRCardController qrCardController;

    @BeforeEach
    void initializeController() {
        Platform.runLater(() -> {
            qrCardController.setFormatLabel(new Label());
            qrCardController.setContentTypeLabel(new Label());
            qrCardController.setRawBytesLabel(new Label());
            qrCardController.setContentFlow(new TextFlow());
            qrCardController.setZipTreeView(new TreeView<>());
            qrCardController.setZipTab(new Tab());
        });
        await().atMost(1, TimeUnit.SECONDS).untilAsserted(() -> Assertions.assertNotNull(qrCardController.getFormatLabel()));
    }

    @Test
    void testSetQRResult() {
        QRCodeResultDto result = new QRCodeResultDto("test", new byte[] {}, 0, QRCodeFormat.QR_CODE, 0);
        qrCardController.setQRResult(result);

        await().atMost(1, TimeUnit.SECONDS).untilAsserted(() -> {
            Assertions.assertEquals(result.getFormat().toString(), qrCardController.getFormatLabel().getText());
            Assertions.assertEquals("Text", qrCardController.getContentTypeLabel().getText());
            Assertions.assertFalse(qrCardController.getContentFlow().getChildren().isEmpty());
        });
    }

    @Test
    void testSetQRResultWithZipPayload() throws Exception {
        java.io.ByteArrayOutputStream output = new java.io.ByteArrayOutputStream();
        try (java.util.zip.ZipOutputStream zos = new java.util.zip.ZipOutputStream(output)) {
            zos.putNextEntry(new java.util.zip.ZipEntry("sample.xml"));
            zos.write("<hello/>".getBytes(java.nio.charset.StandardCharsets.UTF_8));
            zos.closeEntry();
            zos.finish();
        }
        QRCodeResultDto result = new QRCodeResultDto("binary", output.toByteArray(), 0, QRCodeFormat.QR_CODE, 0);
        qrCardController.setQRResult(result);

        await().atMost(2, TimeUnit.SECONDS).untilAsserted(() -> {
            Assertions.assertEquals("ZIP", qrCardController.getContentTypeLabel().getText());
            Assertions.assertFalse(qrCardController.getZipTab().isDisable());
            Assertions.assertNotNull(qrCardController.getZipTreeView().getRoot());
        });
    }

    @Test
    void testOpenDocumentViewer() {
        net.talaatharb.screensnapqr.ui.content.ZipNode fileNode = new net.talaatharb.screensnapqr.ui.content.ZipNode(
                "test.json", false, null, "folder/test.json", "{\"key\":\"value\"}".getBytes(java.nio.charset.StandardCharsets.UTF_8));

        interact(() -> {
            Assertions.assertDoesNotThrow(() -> qrCardController.openDocumentViewer(fileNode));
        });
    }

}
