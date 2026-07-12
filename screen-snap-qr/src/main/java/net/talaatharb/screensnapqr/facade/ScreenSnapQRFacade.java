package net.talaatharb.screensnapqr.facade;

import java.awt.Rectangle;
import java.util.List;

import net.talaatharb.screensnapqr.dtos.QRCodeResultDto;

public interface ScreenSnapQRFacade {

	List<QRCodeResultDto> getAllQRCodesFromScreen() throws Exception;
	List<QRCodeResultDto> getAllQRCodesFromScreen(Rectangle captureBounds) throws Exception;
}
