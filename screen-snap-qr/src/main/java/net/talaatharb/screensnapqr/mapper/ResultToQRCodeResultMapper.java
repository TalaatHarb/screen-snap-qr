package net.talaatharb.screensnapqr.mapper;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.Result;
import com.google.zxing.ResultMetadataType;

import net.talaatharb.screensnapqr.constants.QRCodeFormat;
import net.talaatharb.screensnapqr.constants.QRCodeMetadata;
import net.talaatharb.screensnapqr.dtos.QRCodeResultDto;

@Mapper(uses = { ResultPointToQRResultPoint.class })
public interface ResultToQRCodeResultMapper {

	@Mapping(source = "resultMetadata", target = "resultMetadata", qualifiedByName = "fromResultMetadata")
	@Mapping(source = "barcodeFormat", target = "format", qualifiedByName = "fromFormat")
	@Mapping(target = "qrCodeImage", ignore = true)
	QRCodeResultDto fromResult(Result result);

	default List<QRCodeResultDto> fromResultArray(Result[] results) {
		if (results == null) {
			return List.of();
		}

		List<QRCodeResultDto> mappedResults = new ArrayList<>(results.length);
		for (var result : results) {
			mappedResults.add(fromResult(result));
		}

		return mappedResults;
	}

	@Named("fromFormat")
	default QRCodeFormat fromFormat(BarcodeFormat barcodeFormat) {
		return QRCodeFormat.valueOf(barcodeFormat.name());
	}

	@Named("fromResultMetadata")
	default Map<QRCodeMetadata, Object> fromResultMetadata(Map<ResultMetadataType, Object> resultMetadata) {
		return resultMetadata.entrySet().stream()
				.collect(Collectors.toMap(entry -> fromMetadataType(entry.getKey()), Map.Entry::getValue));
	}

	static QRCodeMetadata fromMetadataType(ResultMetadataType metadata) {
		return QRCodeMetadata.valueOf(metadata.name());
	}
}
