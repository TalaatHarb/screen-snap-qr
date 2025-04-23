package net.talaatharb.screensnapqr.service;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import com.google.zxing.BinaryBitmap;
import com.google.zxing.DecodeHintType;
import com.google.zxing.LuminanceSource;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.NotFoundException;
import com.google.zxing.Result;
import com.google.zxing.client.j2se.BufferedImageLuminanceSource;
import com.google.zxing.common.HybridBinarizer;
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
            r.setQrCodeImage(extractQRCodeRegion(points, image));
        });

    }

    private Result[] decode(BufferedImage image) throws IOException {
        // https://github.com/zxing/zxing/blob/master/javase/src/main/java/com/google/zxing/client/j2se/DecodeWorker.java#L56
        LuminanceSource source = new BufferedImageLuminanceSource(image);

        BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(source));

        MultiFormatReader multiFormatReader = new MultiFormatReader();
        Map<DecodeHintType, Object> hints = new EnumMap<>(DecodeHintType.class);
//        hints.put(DecodeHintType.POSSIBLE_FORMATS, BarcodeFormat.DATA_MATRIX);
        hints.put(DecodeHintType.TRY_HARDER, Boolean.TRUE);
        multiFormatReader.setHints(hints);
        Result[] results;
        try {
            MultipleBarcodeReader reader = new GenericMultipleBarcodeReader(multiFormatReader);
            results = reader.decodeMultiple(bitmap, hints);
        } catch (NotFoundException e) {
            log.warn("No barcode found, {}", e.getMessage());
            return new Result[] {};
        }

        return results;
    }
    
    private static final BufferedImage extractQRCodeRegion(QRResultPointDto[] points, BufferedImage sourceImage) {
        if (points == null || points.length < 3) {
            throw new IllegalArgumentException("At least 3 ResultPoints are required for a QR Code.");
        }

        // Extract the three main points: top-left, top-right, bottom-left
        var topLeft = points[1];
        var topRight = points[0];
        var bottomLeft = points[2];

        // Vector math to estimate bottom-right
        float dx1 = bottomLeft.getX() - topLeft.getX();
        float dy1 = bottomLeft.getY() - topLeft.getY();

        float dx2 = topRight.getX() - topLeft.getX();
        float dy2 = topRight.getY() - topLeft.getY();

        float bottomRightX = topLeft.getX() + dx1 + dx2;
        float bottomRightY = topLeft.getY() + dy1 + dy2;

        float[] xs = { topLeft.getX(), topRight.getX(), bottomLeft.getX(), bottomRightX };
        float[] ys = { topLeft.getY(), topRight.getY(), bottomLeft.getY(), bottomRightY };

        // Compute bounds
        float minX = Float.MAX_VALUE;
        float maxX = Float.MIN_VALUE;
        float minY = Float.MAX_VALUE;
        float maxY = Float.MIN_VALUE;
        
        for (float x : xs) {
            minX = Math.min(minX, x);
            maxX = Math.max(maxX, x);
        }
        for (float y : ys) {
            minY = Math.min(minY, y);
            maxY = Math.max(maxY, y);
        }
        
        minX = minX - dx1/4;
        maxX = maxX + dx1/4;
        minY = minY - dy2/4;
        maxY = maxY + dy2/4;

        // Clamp to image bounds
        int x = Math.max(0, (int) minX);
        int y = Math.max(0, (int) minY);
        int width = Math.min(sourceImage.getWidth() - x, (int) (maxX - minX));
        int height = Math.min(sourceImage.getHeight() - y, (int) (maxY - minY));

        return sourceImage.getSubimage(x, y, width, height);
    }
}
