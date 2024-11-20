package net.talaatharb.screensnapqr.facade;

import java.util.List;

import net.talaatharb.screensnapqr.dtos.QRCodeResultDto;

public interface ScreenSnapQRFacade {

	List<QRCodeResultDto> getAllQRCodesFromScreen() throws Exception;
}
