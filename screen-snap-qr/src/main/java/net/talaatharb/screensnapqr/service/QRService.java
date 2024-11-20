package net.talaatharb.screensnapqr.service;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.List;

import net.talaatharb.screensnapqr.dtos.QRCodeResultDto;

public interface QRService {

	List<QRCodeResultDto> getAllQRCodeContents(BufferedImage image) throws IOException;
}
