package net.talaatharb.screensnapqr.facade;

import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.List;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.talaatharb.screensnapqr.dtos.QRCodeResultDto;
import net.talaatharb.screensnapqr.service.ImportService;
import net.talaatharb.screensnapqr.service.QRService;
import net.talaatharb.screensnapqr.service.ScreenSnapService;

@Slf4j
@RequiredArgsConstructor
public class ScreenSnapQRFacadeImpl implements ScreenSnapQRFacade {

	private final ScreenSnapService screenSnapService;
	private final QRService qrService;
	private final ImportService importService;

	@Override
	public List<QRCodeResultDto> getAllQRCodesFromScreen() throws Exception{
		var image = screenSnapService.takeSnapshot();
		return qrService.getAllQRCodeContents(image);
	}

	@Override
	public List<QRCodeResultDto> getAllQRCodesFromScreen(Rectangle captureBounds) throws Exception {
		var image = screenSnapService.takeSnapshot(captureBounds);
		return qrService.getAllQRCodeContents(image);
	}

	@Override
	public List<QRCodeResultDto> getAllQRCodesFromImage(BufferedImage image) throws Exception {
		return importService.importFromImage(image);
	}

	@Override
	public List<QRCodeResultDto> getAllQRCodesFromImageFile(File file) throws Exception {
		return importService.importFromImageFile(file);
	}

	@Override
	public List<QRCodeResultDto> getAllQRCodesFromPdfFile(File file) throws Exception {
		return importService.importFromPdfFile(file);
	}

	@Override
	public List<QRCodeResultDto> getAllQRCodesFromPdfFile(File file, int startPage, int endPage) throws Exception {
		return importService.importFromPdfFile(file, startPage, endPage);
	}

	@Override
	public List<QRCodeResultDto> getAllQRCodesFromClipboard() throws Exception {
		return importService.importFromClipboard();
	}
}
