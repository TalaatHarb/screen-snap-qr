package net.talaatharb.screensnapqr.ui.content;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.junit.jupiter.api.Test;

import net.talaatharb.screensnapqr.constants.QRCodeFormat;
import net.talaatharb.screensnapqr.dtos.QRCodeResultDto;

class ScannedContentAnalyzerTest {

    private final ScannedContentAnalyzer analyzer = new ScannedContentAnalyzer();

    @Test
    void detectsJsonPayload() {
        QRCodeResultDto result = new QRCodeResultDto("{\"name\":\"talaat\",\"age\":5}", null, 0, QRCodeFormat.QR_CODE, 0);

        ScannedContentViewModel model = analyzer.analyze(result);

        assertEquals(ScannedContentType.JSON, model.getContentType());
        assertNotNull(model.getTokens());
    }

    @Test
    void detectsHyperlinkPayload() {
        QRCodeResultDto result = new QRCodeResultDto("https://github.com/TalaatHarb/screen-snap-qr", null, 0,
                QRCodeFormat.QR_CODE, 0);

        ScannedContentViewModel model = analyzer.analyze(result);

        assertEquals(ScannedContentType.HYPERLINK, model.getContentType());
    }

    @Test
    void detectsXmlPayload() {
        QRCodeResultDto result = new QRCodeResultDto("<root><item id=\"1\">value</item></root>", null, 0,
                QRCodeFormat.QR_CODE, 0);

        ScannedContentViewModel model = analyzer.analyze(result);

        assertEquals(ScannedContentType.XML, model.getContentType());
        assertNotNull(model.getCopyableText());
    }

    @Test
    void detectsYamlPayload() {
        QRCodeResultDto result = new QRCodeResultDto("name: talaat\nversion: 1\nnested:\n  key: value", null, 0,
                QRCodeFormat.QR_CODE, 0);

        ScannedContentViewModel model = analyzer.analyze(result);

        assertEquals(ScannedContentType.YAML, model.getContentType());
    }

    @Test
    void detectsZipPayloadFromRawBytes() throws IOException {
        QRCodeResultDto result = new QRCodeResultDto("binary", buildZipBytes(), 0, QRCodeFormat.QR_CODE, 0);

        ScannedContentViewModel model = analyzer.analyze(result);

        assertEquals(ScannedContentType.ZIP, model.getContentType());
        assertNotNull(model.getZipNode());
        assertEquals("archive.zip", model.getZipNode().getLabel());
    }

    @Test
    void detectsZipPayloadFromPkTextSignature() throws IOException {
        byte[] zipBytes = buildZipBytes();
        String qrText = new String(zipBytes, StandardCharsets.ISO_8859_1);
        QRCodeResultDto result = new QRCodeResultDto(qrText, null, 0, QRCodeFormat.QR_CODE, 0);

        ScannedContentViewModel model = analyzer.analyze(result);

        assertEquals(ScannedContentType.ZIP, model.getContentType());
        assertNotNull(model.getZipNode());
        assertFalse(model.getZipNode().getChildren().isEmpty());
    }

    private static byte[] buildZipBytes() throws IOException {
        try (ByteArrayOutputStream output = new ByteArrayOutputStream();
                ZipOutputStream zipOutputStream = new ZipOutputStream(output)) {
            zipOutputStream.putNextEntry(new ZipEntry("docs/"));
            zipOutputStream.closeEntry();
            zipOutputStream.putNextEntry(new ZipEntry("docs/readme.txt"));
            zipOutputStream.write("hello".getBytes(StandardCharsets.UTF_8));
            zipOutputStream.closeEntry();
            zipOutputStream.finish();
            return output.toByteArray();
        }
    }
}
