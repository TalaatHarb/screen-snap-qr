package net.talaatharb.screensnapqr.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.List;
import javax.imageio.ImageIO;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.graphics.image.LosslessFactory;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import net.talaatharb.screensnapqr.constants.QRCodeFormat;
import net.talaatharb.screensnapqr.dtos.QRCodeResultDto;

@ExtendWith(MockitoExtension.class)
class ImportServiceImplTest {

    @Mock
    private QRService qrService;

    @Mock
    private ClipboardService clipboardService;

    @InjectMocks
    private ImportServiceImpl importService;

    @Test
    void testImportFromImageNullReturnsEmptyList() throws IOException {
        List<QRCodeResultDto> results = importService.importFromImage(null);
        assertTrue(results.isEmpty());
    }

    @Test
    void testImportFromImageValidImageDelegatesToQRService() throws IOException {
        BufferedImage image = new BufferedImage(100, 100, BufferedImage.TYPE_INT_RGB);
        QRCodeResultDto dto = new QRCodeResultDto("text", new byte[]{}, 0, QRCodeFormat.QR_CODE, 0);
        when(qrService.getAllQRCodeContents(image)).thenReturn(List.of(dto));

        List<QRCodeResultDto> results = importService.importFromImage(image);
        assertEquals(1, results.size());
        assertEquals(dto, results.get(0));
    }

    @Test
    void testImportFromImageFileNullOrMissingThrows() {
        assertThrows(IllegalArgumentException.class, () -> importService.importFromImageFile(null));
        assertThrows(IllegalArgumentException.class, () -> importService.importFromImageFile(new File("non_existent_file.png")));
    }

    @Test
    void testImportFromImageFileValidImage(@TempDir Path tempDir) throws IOException {
        BufferedImage img = new BufferedImage(50, 50, BufferedImage.TYPE_INT_RGB);
        File imageFile = tempDir.resolve("test.png").toFile();
        ImageIO.write(img, "png", imageFile);

        QRCodeResultDto dto = new QRCodeResultDto("decoded", new byte[]{}, 0, QRCodeFormat.QR_CODE, 0);
        when(qrService.getAllQRCodeContents(any(BufferedImage.class))).thenReturn(List.of(dto));

        List<QRCodeResultDto> results = importService.importFromImageFile(imageFile);
        assertEquals(1, results.size());
        assertEquals("decoded", results.get(0).getText());
    }

    @Test
    void testImportFromImageFileCorruptFileThrows(@TempDir Path tempDir) throws IOException {
        File corruptFile = tempDir.resolve("corrupt.png").toFile();
        Files.writeString(corruptFile.toPath(), "not an image content");

        assertThrows(IllegalArgumentException.class, () -> importService.importFromImageFile(corruptFile));
    }

    @Test
    void testImportFromPdfFileNullOrMissingThrows() {
        assertThrows(IllegalArgumentException.class, () -> importService.importFromPdfFile(null));
        assertThrows(IllegalArgumentException.class, () -> importService.importFromPdfFile(new File("non_existent.pdf")));
    }

    @Test
    void testImportFromPdfFileValid(@TempDir Path tempDir) throws IOException {
        File pdfFile = tempDir.resolve("test.pdf").toFile();
        try (PDDocument doc = new PDDocument()) {
            PDPage page = new PDPage();
            doc.addPage(page);
            doc.save(pdfFile);
        }

        QRCodeResultDto dto = new QRCodeResultDto("from_pdf", new byte[]{}, 0, QRCodeFormat.QR_CODE, 0);
        when(qrService.getAllQRCodeContents(any(BufferedImage.class))).thenReturn(List.of(dto));

        List<QRCodeResultDto> results = importService.importFromPdfFile(pdfFile);
        assertEquals(1, results.size());
        assertEquals("from_pdf", results.get(0).getText());
    }

    @Test
    void testImportFromPdfFileWithEmbeddedImage(@TempDir Path tempDir) throws IOException {
        File pdfFile = tempDir.resolve("embedded.pdf").toFile();
        try (PDDocument doc = new PDDocument()) {
            PDPage page = new PDPage();
            doc.addPage(page);
            BufferedImage img = new BufferedImage(40, 40, BufferedImage.TYPE_INT_RGB);
            PDImageXObject pdImg = LosslessFactory.createFromImage(doc, img);
            try (PDPageContentStream contentStream = new PDPageContentStream(doc, page)) {
                contentStream.drawImage(pdImg, 50, 50);
            }
            doc.save(pdfFile);
        }

        QRCodeResultDto dto = new QRCodeResultDto("embedded_qr", new byte[]{}, 0, QRCodeFormat.QR_CODE, 0);
        when(qrService.getAllQRCodeContents(any(BufferedImage.class))).thenReturn(List.of(dto));

        List<QRCodeResultDto> results = importService.importFromPdfFile(pdfFile);
        assertEquals(1, results.size());
        assertEquals("embedded_qr", results.get(0).getText());
    }

    @Test
    void testImportFromPdfFileFallbackSecondaryDpi(@TempDir Path tempDir) throws IOException {
        File pdfFile = tempDir.resolve("fallback.pdf").toFile();
        try (PDDocument doc = new PDDocument()) {
            PDPage page = new PDPage();
            doc.addPage(page);
            doc.save(pdfFile);
        }

        QRCodeResultDto dto = new QRCodeResultDto("secondary_qr", new byte[]{}, 0, QRCodeFormat.QR_CODE, 0);
        when(qrService.getAllQRCodeContents(any(BufferedImage.class)))
                .thenReturn(List.of()) // primary DPI returns empty
                .thenReturn(List.of(dto)); // secondary DPI returns result

        List<QRCodeResultDto> results = importService.importFromPdfFile(pdfFile);
        assertEquals(1, results.size());
        assertEquals("secondary_qr", results.get(0).getText());
    }

    @Test
    void testImportFromClipboardWithImage() throws IOException {
        BufferedImage img = new BufferedImage(30, 30, BufferedImage.TYPE_INT_RGB);
        when(clipboardService.getImage()).thenReturn(img);
        QRCodeResultDto dto = new QRCodeResultDto("clipboard_image", new byte[]{}, 0, QRCodeFormat.QR_CODE, 0);
        when(qrService.getAllQRCodeContents(img)).thenReturn(List.of(dto));

        List<QRCodeResultDto> results = importService.importFromClipboard();
        assertEquals(1, results.size());
        assertEquals("clipboard_image", results.get(0).getText());
    }

    @Test
    void testImportFromClipboardWithFiles(@TempDir Path tempDir) throws IOException {
        when(clipboardService.getImage()).thenReturn(null);

        File imgFile = tempDir.resolve("clip.png").toFile();
        ImageIO.write(new BufferedImage(40, 40, BufferedImage.TYPE_INT_RGB), "png", imgFile);

        File pdfFile = tempDir.resolve("clip.pdf").toFile();
        try (PDDocument doc = new PDDocument()) {
            doc.addPage(new PDPage());
            doc.save(pdfFile);
        }

        when(clipboardService.getFiles()).thenReturn(List.of(imgFile, pdfFile));

        QRCodeResultDto dto1 = new QRCodeResultDto("clip1", new byte[]{}, 0, QRCodeFormat.QR_CODE, 0);
        QRCodeResultDto dto2 = new QRCodeResultDto("clip2", new byte[]{}, 0, QRCodeFormat.QR_CODE, 0);
        when(qrService.getAllQRCodeContents(any(BufferedImage.class))).thenReturn(List.of(dto1), List.of(dto2));

        List<QRCodeResultDto> results = importService.importFromClipboard();
        assertEquals(2, results.size());
    }

    @Test
    void testImportFromClipboardWithBase64DataUrl() throws IOException {
        when(clipboardService.getImage()).thenReturn(null);
        when(clipboardService.getFiles()).thenReturn(List.of());

        BufferedImage bImg = new BufferedImage(20, 20, BufferedImage.TYPE_INT_RGB);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(bImg, "png", baos);
        String base64 = Base64.getEncoder().encodeToString(baos.toByteArray());
        String dataUrl = "data:image/png;base64," + base64;

        when(clipboardService.getText()).thenReturn(dataUrl);
        QRCodeResultDto dto = new QRCodeResultDto("base64_qr", new byte[]{}, 0, QRCodeFormat.QR_CODE, 0);
        when(qrService.getAllQRCodeContents(any(BufferedImage.class))).thenReturn(List.of(dto));

        List<QRCodeResultDto> results = importService.importFromClipboard();
        assertEquals(1, results.size());
        assertEquals("base64_qr", results.get(0).getText());
    }

    @Test
    void testImportFromClipboardWithFilePathText(@TempDir Path tempDir) throws IOException {
        when(clipboardService.getImage()).thenReturn(null);
        when(clipboardService.getFiles()).thenReturn(List.of());

        File imgFile = tempDir.resolve("from_path.png").toFile();
        ImageIO.write(new BufferedImage(30, 30, BufferedImage.TYPE_INT_RGB), "png", imgFile);

        when(clipboardService.getText()).thenReturn(imgFile.getAbsolutePath());
        QRCodeResultDto dto = new QRCodeResultDto("from_path_result", new byte[]{}, 0, QRCodeFormat.QR_CODE, 0);
        when(qrService.getAllQRCodeContents(any(BufferedImage.class))).thenReturn(List.of(dto));

        List<QRCodeResultDto> results = importService.importFromClipboard();
        assertEquals(1, results.size());
        assertEquals("from_path_result", results.get(0).getText());
    }

    @Test
    void testImportFromClipboardEmpty() throws IOException {
        when(clipboardService.getImage()).thenReturn(null);
        when(clipboardService.getFiles()).thenReturn(List.of());
        when(clipboardService.getText()).thenReturn(null);

        List<QRCodeResultDto> results = importService.importFromClipboard();
        assertTrue(results.isEmpty());
    }
}
