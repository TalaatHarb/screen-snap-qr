package net.talaatharb.screensnapqr.facade;

import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.List;

import net.talaatharb.screensnapqr.dtos.QRCodeResultDto;

public interface ScreenSnapQRFacade {

	List<QRCodeResultDto> getAllQRCodesFromScreen() throws Exception;
	List<QRCodeResultDto> getAllQRCodesFromScreen(Rectangle captureBounds) throws Exception;
	List<QRCodeResultDto> getAllQRCodesFromImage(BufferedImage image) throws Exception;
	List<QRCodeResultDto> getAllQRCodesFromImageFile(File file) throws Exception;
	List<QRCodeResultDto> getAllQRCodesFromPdfFile(File file) throws Exception;
	List<QRCodeResultDto> getAllQRCodesFromPdfFile(File file, int startPage, int endPage) throws Exception;
	List<QRCodeResultDto> getAllQRCodesFromClipboard() throws Exception;
}

