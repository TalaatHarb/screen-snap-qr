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
import net.talaatharb.screensnapqr.mapper.ResultToQRCodeResultMapper;

@Slf4j
@RequiredArgsConstructor
public class QRServiceImpl implements QRService {

	private final ResultToQRCodeResultMapper mapper;

	@Override
	public List<QRCodeResultDto> getAllQRCodeContents(BufferedImage image) throws IOException {
		var results = decode(image);
		return mapper.fromResultArray(results);
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
}
