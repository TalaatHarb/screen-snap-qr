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
import net.talaatharb.screensnapqr.ui.content.ScannedContentType;

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

    @Test
    void testOpenPrescriptionViewerWithMatchingSchema() {
        final String prescriptionXml = "<P xmlns=\"http://www.cnas.ro/pel/1.0\" SC=\"AB\" SN=\"1234567\" PS=\"12345\" "
                + "CC=\"999888\" CN=\"CT-01\" OU=\"CASMB\"><PD FN=\"Ion\" LN=\"Popescu\"/></P>";
        net.talaatharb.screensnapqr.ui.content.ZipNode fileNode = new net.talaatharb.screensnapqr.ui.content.ZipNode(
                "prescription.xml", false, null, "prescription.xml",
                prescriptionXml.getBytes(java.nio.charset.StandardCharsets.UTF_8));

        interact(() -> {
            Assertions.assertDoesNotThrow(() -> qrCardController.openPrescriptionViewer(fileNode));
        });
    }

    @Test
    void testOpenPrescriptionViewerWithNonMatchingContentDoesNothing() {
        net.talaatharb.screensnapqr.ui.content.ZipNode fileNode = new net.talaatharb.screensnapqr.ui.content.ZipNode(
                "test.json", false, null, "folder/test.json", "{\"key\":\"value\"}".getBytes(java.nio.charset.StandardCharsets.UTF_8));

        interact(() -> {
            Assertions.assertDoesNotThrow(() -> qrCardController.openPrescriptionViewer(fileNode));
        });
    }

    @Test
    void testOpenInvoiceViewerWithMatchingSchema() {
        final String invoiceXml = "<Invoice xmlns=\"urn:oasis:names:specification:ubl:schema:xsd:Invoice-2\">"
                + "<ID>INV-1</ID><IssueDate>2024-01-01</IssueDate><DocumentCurrencyCode>EUR</DocumentCurrencyCode>"
                + "<AccountingSupplierParty/><AccountingCustomerParty/><LegalMonetaryTotal/></Invoice>";
        net.talaatharb.screensnapqr.ui.content.ZipNode fileNode = new net.talaatharb.screensnapqr.ui.content.ZipNode(
                "invoice.xml", false, null, "invoice.xml", invoiceXml.getBytes(java.nio.charset.StandardCharsets.UTF_8));

        interact(() -> {
            Assertions.assertDoesNotThrow(() -> qrCardController.openInvoiceViewer(fileNode));
        });
    }

    @Test
    void testOpenRenderedPrescriptionViewerUsesScannedContentDirectly() {
        final String prescriptionXml = "<P xmlns=\"http://www.cnas.ro/pel/1.0\" SC=\"AB\" SN=\"1234567\" PS=\"12345\" "
                + "CC=\"999888\" CN=\"CT-01\" OU=\"CASMB\"><PD FN=\"Ion\" LN=\"Popescu\"/></P>";
        QRCodeResultDto result = new QRCodeResultDto(prescriptionXml, prescriptionXml.getBytes(java.nio.charset.StandardCharsets.UTF_8),
                0, QRCodeFormat.QR_CODE, 0);
        qrCardController.setQRResult(result);

        await().atMost(2, TimeUnit.SECONDS).untilAsserted(() -> Assertions.assertEquals("XML",
                qrCardController.getContentTypeLabel().getText()));

        interact(() -> Assertions.assertDoesNotThrow(() -> qrCardController.openRenderedPrescriptionViewer()));
    }

    @Test
    void testOpenRenderedInvoiceViewerUsesScannedContentDirectly() {
        final String invoiceXml = "<Invoice xmlns=\"urn:oasis:names:specification:ubl:schema:xsd:Invoice-2\">"
                + "<ID>INV-1</ID><IssueDate>2024-01-01</IssueDate><DocumentCurrencyCode>EUR</DocumentCurrencyCode>"
                + "<AccountingSupplierParty/><AccountingCustomerParty/><LegalMonetaryTotal/></Invoice>";
        QRCodeResultDto result = new QRCodeResultDto(invoiceXml, invoiceXml.getBytes(java.nio.charset.StandardCharsets.UTF_8),
                0, QRCodeFormat.QR_CODE, 0);
        qrCardController.setQRResult(result);

        await().atMost(2, TimeUnit.SECONDS).untilAsserted(() -> Assertions.assertEquals("XML",
                qrCardController.getContentTypeLabel().getText()));

        interact(() -> Assertions.assertDoesNotThrow(() -> qrCardController.openRenderedInvoiceViewer()));
    }

    @Test
    void testOpenRenderedDocumentViewerWithPlainTextDoesNotThrow() {
        QRCodeResultDto result = new QRCodeResultDto("test", new byte[] {}, 0, QRCodeFormat.QR_CODE, 0);
        qrCardController.setQRResult(result);

        await().atMost(1, TimeUnit.SECONDS).untilAsserted(() -> Assertions.assertEquals("Text",
                qrCardController.getContentTypeLabel().getText()));

        interact(() -> Assertions.assertDoesNotThrow(() -> qrCardController.openRenderedDocumentViewer()));
    }

    @Test
    void testSyntheticExtensionMatchesDetectedContentType() {
        Assertions.assertEquals(".json", QRCardController.syntheticExtensionFor(ScannedContentType.JSON));
        Assertions.assertEquals(".xml", QRCardController.syntheticExtensionFor(ScannedContentType.XML));
        Assertions.assertEquals(".txt", QRCardController.syntheticExtensionFor(ScannedContentType.TEXT));
    }

    @Test
    void testOpenRenderedDocumentViewerAppliesJsonSyntaxHighlighting() {
        final String jsonText = "{\"key\":\"value\"}";
        QRCodeResultDto result = new QRCodeResultDto(jsonText, jsonText.getBytes(java.nio.charset.StandardCharsets.UTF_8), 0,
                QRCodeFormat.QR_CODE, 0);
        qrCardController.setQRResult(result);

        await().atMost(2, TimeUnit.SECONDS).untilAsserted(() -> Assertions.assertEquals("JSON",
                qrCardController.getContentTypeLabel().getText()));

        final java.util.concurrent.atomic.AtomicReference<javafx.stage.Stage> stageRef = new java.util.concurrent.atomic.AtomicReference<>();
        interact(() -> stageRef.set(net.talaatharb.screensnapqr.ui.viewer.DocumentViewer.show(
                "Scanned content" + QRCardController.syntheticExtensionFor(ScannedContentType.JSON), jsonText)));

        interact(() -> {
            final org.fxmisc.richtext.CodeArea codeArea = (org.fxmisc.richtext.CodeArea) ((javafx.scene.layout.VBox) stageRef
                    .get().getScene().getRoot()).getChildren().stream()
                    .filter(node -> node instanceof org.fxmisc.flowless.VirtualizedScrollPane)
                    .findFirst().map(node -> ((org.fxmisc.flowless.VirtualizedScrollPane<?>) node).getContent())
                    .orElseThrow();
            Assertions.assertFalse(codeArea.getStyleSpans(0, codeArea.getLength()).styleStream()
                    .allMatch(java.util.Collection::isEmpty));
            stageRef.get().close();
        });
    }

}
