package net.talaatharb.screensnapqr.service;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.EnumSet;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.BinaryBitmap;
import com.google.zxing.Binarizer;
import com.google.zxing.ChecksumException;
import com.google.zxing.DecodeHintType;
import com.google.zxing.FormatException;
import com.google.zxing.InvertedLuminanceSource;
import com.google.zxing.LuminanceSource;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.NotFoundException;
import com.google.zxing.Reader;
import com.google.zxing.Result;
import com.google.zxing.ResultPoint;
import com.google.zxing.client.j2se.BufferedImageLuminanceSource;
import com.google.zxing.common.GlobalHistogramBinarizer;
import com.google.zxing.common.HybridBinarizer;
import com.google.zxing.datamatrix.DataMatrixReader;
import com.google.zxing.multi.ByQuadrantReader;
import com.google.zxing.multi.GenericMultipleBarcodeReader;
import com.google.zxing.multi.MultipleBarcodeReader;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.talaatharb.screensnapqr.dtos.QRCodeResultDto;
import net.talaatharb.screensnapqr.dtos.QRResultPointDto;
import net.talaatharb.screensnapqr.mapper.ResultToQRCodeResultMapper;

@Slf4j
@RequiredArgsConstructor
public class QRServiceImpl implements QRService {

    private static final Set<BarcodeFormat> NON_DATA_MATRIX_FORMATS = EnumSet.complementOf(EnumSet.of(BarcodeFormat.DATA_MATRIX));
    private static final int DATA_MATRIX_UPSCALE_FACTOR = 2;
    private static final long MAX_PIXELS_FOR_UPSCALE = 2_000_000L;
    private static final int[] TILE_GRID_SIZES = { 2, 3, 4 };
    private static final double TILE_OVERLAP_RATIO = 0.15;

    private final ResultToQRCodeResultMapper mapper;

    @Override
    public List<QRCodeResultDto> getAllQRCodeContents(BufferedImage image) throws IOException {
        var results = decode(image);
        List<QRCodeResultDto> resultList = mapper.fromResultArray(results);
        putQRCodeImagePortion(image, resultList);
        return resultList;
    }

    private void putQRCodeImagePortion(final BufferedImage image, List<QRCodeResultDto> resultList) {
        resultList.forEach(r -> {
            var points = r.getResultPoints();
            r.setQrCodeImage(extractCodeRegion(points, image));
        });

    }

    private Result[] decode(BufferedImage image) throws IOException {
        // https://github.com/zxing/zxing/blob/master/javase/src/main/java/com/google/zxing/client/j2se/DecodeWorker.java#L56
        LuminanceSource source = new BufferedImageLuminanceSource(image);

        BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(source));

        final var standardHints = new EnumMap<DecodeHintType, Object>(DecodeHintType.class);
        standardHints.put(DecodeHintType.TRY_HARDER, Boolean.TRUE);
        standardHints.put(DecodeHintType.POSSIBLE_FORMATS, new ArrayList<>(NON_DATA_MATRIX_FORMATS));

        final var dataMatrixHints = new EnumMap<DecodeHintType, Object>(DecodeHintType.class);
        dataMatrixHints.put(DecodeHintType.TRY_HARDER, Boolean.TRUE);
        dataMatrixHints.put(DecodeHintType.POSSIBLE_FORMATS, List.of(BarcodeFormat.DATA_MATRIX));

        final Result[] standardResults = decodeWithHints(bitmap, standardHints, "standard");
        final Result[] dataMatrixResults = decodeDataMatrixWithPreprocessing(image, dataMatrixHints);

        return mergeUniqueResults(standardResults, dataMatrixResults);
    }

    private Result[] decodeDataMatrixWithPreprocessing(BufferedImage image, Map<DecodeHintType, Object> dataMatrixHints) {
        final LinkedHashMap<String, Result> unique = new LinkedHashMap<>();

        scanDataMatrixVariant(unique, image, dataMatrixHints, "original", true, 1.0F);

        final BufferedImage grayscale = toGrayscale(image);
        scanDataMatrixVariant(unique, grayscale, dataMatrixHints, "grayscale", true, 1.0F);

        final BufferedImage otsuThreshold = toOtsuBinary(grayscale);
        scanDataMatrixVariant(unique, otsuThreshold, dataMatrixHints, "otsu-binary", true, 1.0F);

        final BufferedImage invertedGray = invert(grayscale);
        scanDataMatrixVariant(unique, invertedGray, dataMatrixHints, "inverted-grayscale", true, 1.0F);

        if ((long) image.getWidth() * image.getHeight() <= MAX_PIXELS_FOR_UPSCALE) {
            final BufferedImage upscaledGray = scale(grayscale, DATA_MATRIX_UPSCALE_FACTOR);
            scanDataMatrixVariant(unique, upscaledGray, dataMatrixHints, "upscaled-grayscale", false,
                    1.0F / DATA_MATRIX_UPSCALE_FACTOR);

            final BufferedImage upscaledBinary = toOtsuBinary(upscaledGray);
            scanDataMatrixVariant(unique, upscaledBinary, dataMatrixHints, "upscaled-otsu-binary", false,
                    1.0F / DATA_MATRIX_UPSCALE_FACTOR);
        }

        if (unique.isEmpty()) {
            log.info("No data-matrix scan results found across preprocessing variants.");
        } else {
            log.info("Detected {} data-matrix result(s) after preprocessing.", unique.size());
        }

        return unique.values().toArray(new Result[0]);
    }

    private void scanDataMatrixVariant(Map<String, Result> unique, BufferedImage image,
            Map<DecodeHintType, Object> dataMatrixHints, String variantName, boolean runTileScan, float scaleToOriginal) {
        addToUnique(unique,
                normalizeResults(decodeWithBinarizer(image, dataMatrixHints, variantName, false, false), scaleToOriginal, 0, 0));
        addToUnique(unique,
                normalizeResults(decodeWithBinarizer(image, dataMatrixHints, variantName, true, false), scaleToOriginal, 0, 0));
        addToUnique(unique,
                normalizeResults(decodeWithBinarizer(image, dataMatrixHints, variantName, false, true), scaleToOriginal, 0, 0));
        addToUnique(unique,
                normalizeResults(decodeWithBinarizer(image, dataMatrixHints, variantName, true, true), scaleToOriginal, 0, 0));

        addToUnique(unique, normalizeResults(
                decodeWithDedicatedDataMatrixReader(image, dataMatrixHints, variantName, false, false), scaleToOriginal, 0, 0));
        addToUnique(unique, normalizeResults(
                decodeWithDedicatedDataMatrixReader(image, dataMatrixHints, variantName, true, false), scaleToOriginal, 0, 0));
        addToUnique(unique, normalizeResults(
                decodeWithDedicatedDataMatrixReader(image, dataMatrixHints, variantName, false, true), scaleToOriginal, 0, 0));
        addToUnique(unique, normalizeResults(
                decodeWithDedicatedDataMatrixReader(image, dataMatrixHints, variantName, true, true), scaleToOriginal, 0, 0));

        if (runTileScan) {
            addToUnique(unique, decodeDataMatrixByTiles(image, dataMatrixHints, variantName));
        }
    }

    private Result[] decodeWithBinarizer(BufferedImage image, Map<DecodeHintType, Object> hints, String variantName,
            boolean inverted, boolean globalHistogram) {
        LuminanceSource source = new BufferedImageLuminanceSource(image);
        if (inverted) {
            source = new InvertedLuminanceSource(source);
        }

        final BinaryBitmap bitmap = globalHistogram ? new BinaryBitmap(new GlobalHistogramBinarizer(source))
                : new BinaryBitmap(new HybridBinarizer(source));
        return decodeWithHints(bitmap, hints, "data-matrix:" + variantName);
    }

    private Result[] decodeWithDedicatedDataMatrixReader(BufferedImage image, Map<DecodeHintType, Object> hints,
            String variantName, boolean inverted, boolean globalHistogram) {
        LuminanceSource source = new BufferedImageLuminanceSource(image);
        if (inverted) {
            source = new InvertedLuminanceSource(source);
        }

        final Binarizer binarizer = globalHistogram ? new GlobalHistogramBinarizer(source) : new HybridBinarizer(source);
        final BinaryBitmap bitmap = new BinaryBitmap(binarizer);
        final String scanName = String.format("data-matrix-reader:%s:%s:%s", variantName,
                inverted ? "inverted" : "normal", globalHistogram ? "global" : "hybrid");

        return decodeSingleDataMatrix(bitmap, hints, scanName);
    }

    private Result[] decodeDataMatrixByTiles(BufferedImage image, Map<DecodeHintType, Object> hints, String variantName) {
        final LinkedHashMap<String, Result> unique = new LinkedHashMap<>();

        for (int gridSize : TILE_GRID_SIZES) {
            final int tileWidth = Math.max(32, image.getWidth() / gridSize);
            final int tileHeight = Math.max(32, image.getHeight() / gridSize);
            final int stepX = Math.max(16, (int) Math.round(tileWidth * (1.0 - TILE_OVERLAP_RATIO)));
            final int stepY = Math.max(16, (int) Math.round(tileHeight * (1.0 - TILE_OVERLAP_RATIO)));

            for (int top = 0; top < image.getHeight(); top += stepY) {
                for (int left = 0; left < image.getWidth(); left += stepX) {
                    final int right = Math.min(image.getWidth(), left + tileWidth);
                    final int bottom = Math.min(image.getHeight(), top + tileHeight);
                    final int width = right - left;
                    final int height = bottom - top;

                    if (width < 24 || height < 24) {
                        continue;
                    }

                    final BufferedImage tile = image.getSubimage(left, top, width, height);
                    final String tileName = String.format("%s:grid%d:x%d:y%d", variantName, gridSize, left, top);
                    addToUnique(unique, normalizeResults(
                            decodeWithDedicatedDataMatrixReader(tile, hints, tileName, false, false), 1.0F, left, top));
                    addToUnique(unique, normalizeResults(
                            decodeWithDedicatedDataMatrixReader(tile, hints, tileName, true, false), 1.0F, left, top));
                    addToUnique(unique, normalizeResults(
                            decodeWithDedicatedDataMatrixReader(tile, hints, tileName, false, true), 1.0F, left, top));
                    addToUnique(unique, normalizeResults(
                            decodeWithDedicatedDataMatrixReader(tile, hints, tileName, true, true), 1.0F, left, top));
                }
            }
        }

        return unique.values().toArray(new Result[0]);
    }

    private Result[] decodeSingleDataMatrix(BinaryBitmap bitmap, Map<DecodeHintType, Object> hints, String scanName) {
        final Reader[] readers = { new DataMatrixReader(), new ByQuadrantReader(new DataMatrixReader()) };
        for (Reader reader : readers) {
            try {
                final Result result = reader.decode(bitmap, hints);
                if (result != null) {
                    return new Result[] { result };
                }
            } catch (NotFoundException e) {
                log.trace("No {} results with {}", scanName, reader.getClass().getSimpleName());
            } catch (ChecksumException | FormatException e) {
                log.trace("{} failed with {}", scanName, e.getClass().getSimpleName());
            } finally {
                reader.reset();
            }
        }
        return new Result[] {};
    }

    private static Result[] normalizeResults(Result[] results, float scaleToOriginal, float offsetX, float offsetY) {
        if (results == null || results.length == 0) {
            return new Result[] {};
        }

        final Result[] normalized = new Result[results.length];
        for (int i = 0; i < results.length; i++) {
            normalized[i] = normalizeResult(results[i], scaleToOriginal, offsetX, offsetY);
        }
        return normalized;
    }

    private static Result normalizeResult(Result result, float scaleToOriginal, float offsetX, float offsetY) {
        final ResultPoint[] points = result.getResultPoints();
        final ResultPoint[] normalizedPoints;

        if (points == null || points.length == 0) {
            normalizedPoints = points;
        } else {
            normalizedPoints = new ResultPoint[points.length];
            for (int i = 0; i < points.length; i++) {
                final ResultPoint point = points[i];
                if (point == null) {
                    continue;
                }
                normalizedPoints[i] = new ResultPoint(point.getX() * scaleToOriginal + offsetX,
                        point.getY() * scaleToOriginal + offsetY);
            }
        }

        final Result normalized = new Result(result.getText(), result.getRawBytes(), normalizedPoints,
                result.getBarcodeFormat(), result.getTimestamp());
        if (result.getResultMetadata() != null) {
            normalized.putAllMetadata(result.getResultMetadata());
        }
        return normalized;
    }

    private Result[] decodeWithHints(BinaryBitmap bitmap, Map<DecodeHintType, Object> hints, String scanName) {
        final MultiFormatReader multiFormatReader = new MultiFormatReader();
        multiFormatReader.setHints(hints);

        try {
            final MultipleBarcodeReader reader = new GenericMultipleBarcodeReader(multiFormatReader);
            return reader.decodeMultiple(bitmap, hints);
        } catch (NotFoundException e) {
            log.debug("No {} scan results found.", scanName);
            return new Result[] {};
        }
    }

    static Result[] mergeUniqueResults(Result[] primaryResults, Result[] secondaryResults) {
        final LinkedHashMap<String, Result> unique = new LinkedHashMap<>();
        addToUnique(unique, primaryResults);
        addToUnique(unique, secondaryResults);
        return unique.values().toArray(new Result[0]);
    }

    private static void addToUnique(Map<String, Result> unique, Result[] results) {
        if (results == null) {
            return;
        }

        for (Result result : results) {
            unique.putIfAbsent(buildResultKey(result), result);
        }
    }

    private static String buildResultKey(Result result) {
        final String format = result.getBarcodeFormat() == null ? "" : result.getBarcodeFormat().name();
        final String text = Objects.toString(result.getText(), "");
        final byte[] rawBytes = result.getRawBytes();
        final String rawKey = rawBytes == null ? "" : Base64.getEncoder().encodeToString(rawBytes);
        final String pointsKey = buildPointsKey(result.getResultPoints());

        return String.join("|", format, text, rawKey, pointsKey);
    }

    private static String buildPointsKey(ResultPoint[] points) {
        if (points == null || points.length == 0) {
            return "";
        }

        return Arrays.stream(points).filter(Objects::nonNull)
                .map(point -> String.format("%d,%d", Math.round(point.getX()), Math.round(point.getY())))
                .collect(Collectors.joining(";"));
    }

    private static BufferedImage toGrayscale(BufferedImage sourceImage) {
        final BufferedImage grayscale = new BufferedImage(sourceImage.getWidth(), sourceImage.getHeight(),
                BufferedImage.TYPE_BYTE_GRAY);
        final Graphics2D graphics = grayscale.createGraphics();
        try {
            graphics.drawImage(sourceImage, 0, 0, null);
        } finally {
            graphics.dispose();
        }
        return grayscale;
    }

    private static BufferedImage invert(BufferedImage sourceImage) {
        final BufferedImage inverted = new BufferedImage(sourceImage.getWidth(), sourceImage.getHeight(),
                BufferedImage.TYPE_BYTE_GRAY);

        for (int y = 0; y < sourceImage.getHeight(); y++) {
            for (int x = 0; x < sourceImage.getWidth(); x++) {
                final int rgb = sourceImage.getRGB(x, y);
                final int gray = rgb & 0xFF;
                final int value = 255 - gray;
                final int newRgb = (0xFF << 24) | (value << 16) | (value << 8) | value;
                inverted.setRGB(x, y, newRgb);
            }
        }

        return inverted;
    }

    private static BufferedImage toOtsuBinary(BufferedImage sourceImage) {
        final int threshold = computeOtsuThreshold(sourceImage);
        final BufferedImage binary = new BufferedImage(sourceImage.getWidth(), sourceImage.getHeight(),
                BufferedImage.TYPE_BYTE_GRAY);

        for (int y = 0; y < sourceImage.getHeight(); y++) {
            for (int x = 0; x < sourceImage.getWidth(); x++) {
                final int rgb = sourceImage.getRGB(x, y);
                final int gray = rgb & 0xFF;
                final int value = gray >= threshold ? 255 : 0;
                final int newRgb = (0xFF << 24) | (value << 16) | (value << 8) | value;
                binary.setRGB(x, y, newRgb);
            }
        }
        return binary;
    }

    private static int computeOtsuThreshold(BufferedImage image) {
        final int[] histogram = new int[256];
        final int totalPixels = image.getWidth() * image.getHeight();

        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                final int rgb = image.getRGB(x, y);
                histogram[rgb & 0xFF]++;
            }
        }

        double sum = 0;
        for (int level = 0; level < 256; level++) {
            sum += (double) level * histogram[level];
        }

        double sumBackground = 0;
        int backgroundWeight = 0;
        int threshold = 0;
        double maxVariance = 0;

        for (int level = 0; level < 256; level++) {
            backgroundWeight += histogram[level];
            if (backgroundWeight == 0) {
                continue;
            }

            final int foregroundWeight = totalPixels - backgroundWeight;
            if (foregroundWeight == 0) {
                break;
            }

            sumBackground += (double) level * histogram[level];
            final double backgroundMean = sumBackground / backgroundWeight;
            final double foregroundMean = (sum - sumBackground) / foregroundWeight;
            final double variance = (double) backgroundWeight * foregroundWeight
                    * Math.pow(backgroundMean - foregroundMean, 2);

            if (variance > maxVariance) {
                maxVariance = variance;
                threshold = level;
            }
        }

        return threshold;
    }

    private static BufferedImage scale(BufferedImage sourceImage, int factor) {
        final int targetWidth = sourceImage.getWidth() * factor;
        final int targetHeight = sourceImage.getHeight() * factor;
        final BufferedImage scaled = new BufferedImage(targetWidth, targetHeight, BufferedImage.TYPE_BYTE_GRAY);

        final Graphics2D graphics = scaled.createGraphics();
        try {
            graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
            graphics.drawImage(sourceImage, 0, 0, targetWidth, targetHeight, null);
        } finally {
            graphics.dispose();
        }

        return scaled;
    }
    
    private static BufferedImage extractCodeRegion(QRResultPointDto[] points, BufferedImage sourceImage) {
        if (points == null || points.length == 0) {
            return null;
        }

        float minX = Float.MAX_VALUE;
        float maxX = Float.MIN_VALUE;
        float minY = Float.MAX_VALUE;
        float maxY = Float.MIN_VALUE;
        int validPoints = 0;

        for (QRResultPointDto point : points) {
            if (point == null) {
                continue;
            }
            validPoints++;
            minX = Math.min(minX, point.getX());
            maxX = Math.max(maxX, point.getX());
            minY = Math.min(minY, point.getY());
            maxY = Math.max(maxY, point.getY());
        }

        if (validPoints == 0) {
            return null;
        }

        float spanX = Math.max(1.0F, maxX - minX);
        float spanY = Math.max(1.0F, maxY - minY);

        // Some formats may return a single or tightly-clustered point; enforce a minimum crop window.
        final float minimumSpan = Math.max(48.0F, Math.min(sourceImage.getWidth(), sourceImage.getHeight()) * 0.06F);
        if (spanX < minimumSpan) {
            final float centerX = (minX + maxX) / 2.0F;
            minX = centerX - minimumSpan / 2.0F;
            maxX = centerX + minimumSpan / 2.0F;
            spanX = minimumSpan;
        }
        if (spanY < minimumSpan) {
            final float centerY = (minY + maxY) / 2.0F;
            minY = centerY - minimumSpan / 2.0F;
            maxY = centerY + minimumSpan / 2.0F;
            spanY = minimumSpan;
        }

        final float paddingX = Math.max(10.0F, spanX * 0.25F);
        final float paddingY = Math.max(10.0F, spanY * 0.25F);

        final int left = Math.max(0, (int) Math.floor(minX - paddingX));
        final int top = Math.max(0, (int) Math.floor(minY - paddingY));
        final int right = Math.min(sourceImage.getWidth(), (int) Math.ceil(maxX + paddingX));
        final int bottom = Math.min(sourceImage.getHeight(), (int) Math.ceil(maxY + paddingY));

        final int width = Math.max(1, right - left);
        final int height = Math.max(1, bottom - top);

        return sourceImage.getSubimage(left, top, width, height);
    }
}
