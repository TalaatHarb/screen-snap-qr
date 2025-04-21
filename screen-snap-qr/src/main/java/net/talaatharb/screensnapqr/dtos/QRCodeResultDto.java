package net.talaatharb.screensnapqr.dtos;

import java.util.Map;

import lombok.Data;
import net.talaatharb.screensnapqr.constants.QRCodeFormat;
import net.talaatharb.screensnapqr.constants.QRCodeMetadata;

@Data
public class QRCodeResultDto {

    private final String text;
    private final byte[] rawBytes;
    private final int numBits;
    private QRResultPointDto[] resultPoints;
    private final QRCodeFormat format;
    private Map<QRCodeMetadata, Object> resultMetadata;
    private final long timestamp;

}
