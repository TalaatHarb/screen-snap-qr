package net.talaatharb.screensnapqr.service;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import javax.imageio.ImageIO;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.ImageType;
import org.apache.pdfbox.rendering.PDFRenderer;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.talaatharb.screensnapqr.dtos.QRCodeResultDto;

@Slf4j
@RequiredArgsConstructor
public class ImportServiceImpl implements ImportService {

    private static final int PRIMARY_PDF_DPI = 300;
    private static final int SECONDARY_PDF_DPI = 200;

    private final QRService qrService;
    private final ClipboardService clipboardService;

    @Override
    public List<QRCodeResultDto> importFromImage(BufferedImage image) throws IOException {
        if (image == null) {
            return List.of();
        }
        return qrService.getAllQRCodeContents(image);
    }

    @Override
    public List<QRCodeResultDto> importFromImageFile(File file) throws IOException {
        if (file == null || !file.exists() || !file.isFile()) {
            throw new IllegalArgumentException("Invalid image file: " + file);
        }
        BufferedImage image = ImageIO.read(file);
        if (image == null) {
            throw new IllegalArgumentException("Unable to decode image from file: " + file.getName());
        }
        return qrService.getAllQRCodeContents(image);
    }

    @Override
    public List<QRCodeResultDto> importFromPdfFile(File file) throws IOException {
        return importFromPdfFile(file, 0, Integer.MAX_VALUE);
    }

    @Override
    public List<QRCodeResultDto> importFromPdfFile(File file, int startPage, int endPage) throws IOException {
        if (file == null || !file.exists() || !file.isFile()) {
            throw new IllegalArgumentException("Invalid PDF file: " + file);
        }
        List<QRCodeResultDto> results = new ArrayList<>();
        try (PDDocument document = Loader.loadPDF(file)) {
            PDFRenderer renderer = new PDFRenderer(document);
            int totalPages = document.getNumberOfPages();
            int start = Math.max(0, startPage);
            int end = Math.min(totalPages - 1, endPage);
            for (int page = start; page <= end; page++) {
                List<QRCodeResultDto> pageResults = new ArrayList<>();
                try {
                    // 1. Scan embedded images from the page if any
                    org.apache.pdfbox.pdmodel.PDPage pdPage = document.getPage(page);
                    mergeUniqueResults(pageResults, extractFromEmbeddedImages(pdPage));

                    // 2. Render page at primary high resolution (300 DPI)
                    BufferedImage pageImage = renderer.renderImageWithDPI(page, PRIMARY_PDF_DPI, ImageType.RGB);
                    mergeUniqueResults(pageResults, qrService.getAllQRCodeContents(pageImage));

                    // 3. Fallback scan at secondary resolution if nothing found
                    if (pageResults.isEmpty()) {
                        BufferedImage secondaryImage = renderer.renderImageWithDPI(page, SECONDARY_PDF_DPI, ImageType.RGB);
                        mergeUniqueResults(pageResults, qrService.getAllQRCodeContents(secondaryImage));
                    }
                } catch (Exception e) {
                    log.warn("Error rendering or scanning PDF page {} of {}: {}", page + 1, totalPages, e.getMessage());
                }
                mergeUniqueResults(results, pageResults);
            }
        }
        return results;
    }

    private List<QRCodeResultDto> extractFromEmbeddedImages(org.apache.pdfbox.pdmodel.PDPage page) {
        List<QRCodeResultDto> embeddedResults = new ArrayList<>();
        try {
            org.apache.pdfbox.pdmodel.PDResources resources = page.getResources();
            if (resources != null) {
                for (org.apache.pdfbox.cos.COSName xName : resources.getXObjectNames()) {
                    org.apache.pdfbox.pdmodel.graphics.PDXObject xObject = resources.getXObject(xName);
                    if (xObject instanceof org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject imgX) {
                        BufferedImage bImg = imgX.getImage();
                        if (bImg != null) {
                            mergeUniqueResults(embeddedResults, qrService.getAllQRCodeContents(bImg));
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.debug("Error extracting embedded images from PDF page: {}", e.getMessage());
        }
        return embeddedResults;
    }

    private void mergeUniqueResults(List<QRCodeResultDto> target, List<QRCodeResultDto> incoming) {
        if (incoming == null || incoming.isEmpty()) {
            return;
        }
        for (QRCodeResultDto item : incoming) {
            if (item == null) {
                continue;
            }
            boolean exists = target.stream().anyMatch(existing ->
                    java.util.Objects.equals(existing.getText(), item.getText()) &&
                    java.util.Objects.equals(existing.getFormat(), item.getFormat()));
            if (!exists) {
                target.add(item);
            }
        }
    }

    @Override
    public List<QRCodeResultDto> importFromClipboard() throws IOException {
        BufferedImage img = clipboardService.getImage();
        if (img != null) {
            return qrService.getAllQRCodeContents(img);
        }

        List<File> files = clipboardService.getFiles();
        if (files != null && !files.isEmpty()) {
            List<QRCodeResultDto> allResults = new ArrayList<>();
            for (File file : files) {
                if (file.getName().toLowerCase().endsWith(".pdf")) {
                    allResults.addAll(importFromPdfFile(file));
                } else {
                    try {
                        allResults.addAll(importFromImageFile(file));
                    } catch (Exception e) {
                        log.warn("Skipping non-image/unsupported clipboard file {}: {}", file.getName(), e.getMessage());
                    }
                }
            }
            return allResults;
        }

        String text = clipboardService.getText();
        if (text != null && !text.isBlank()) {
            BufferedImage base64Image = tryDecodeBase64Image(text.trim());
            if (base64Image != null) {
                return qrService.getAllQRCodeContents(base64Image);
            }
            File candidateFile = new File(text.trim());
            if (candidateFile.exists() && candidateFile.isFile()) {
                if (candidateFile.getName().toLowerCase().endsWith(".pdf")) {
                    return importFromPdfFile(candidateFile);
                } else {
                    try {
                        return importFromImageFile(candidateFile);
                    } catch (Exception e) {
                        log.debug("Clipboard text file is not a supported image: {}", text);
                    }
                }
            }
        }

        return List.of();
    }

    private BufferedImage tryDecodeBase64Image(String text) {
        try {
            String base64Data = text;
            if (base64Data.startsWith("data:image/") && base64Data.contains("base64,")) {
                base64Data = base64Data.substring(base64Data.indexOf("base64,") + 7);
            }
            byte[] bytes = Base64.getDecoder().decode(base64Data);
            try (ByteArrayInputStream in = new ByteArrayInputStream(bytes)) {
                return ImageIO.read(in);
            }
        } catch (Exception e) {
            return null;
        }
    }
}
