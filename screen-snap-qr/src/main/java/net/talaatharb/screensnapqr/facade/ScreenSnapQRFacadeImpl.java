package net.talaatharb.screensnapqr.facade;

import java.util.List;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.talaatharb.screensnapqr.dtos.QRCodeResultDto;
import net.talaatharb.screensnapqr.service.QRService;
import net.talaatharb.screensnapqr.service.ScreenSnapService;

@Slf4j
@RequiredArgsConstructor
public class ScreenSnapQRFacadeImpl implements ScreenSnapQRFacade {

	private final ScreenSnapService screenSnapService;
	private final QRService qrService;

	@Override
	public List<QRCodeResultDto> getAllQRCodesFromScreen() throws Exception{
		var image = screenSnapService.takeSnapshot();
		return qrService.getAllQRCodeContents(image);
	}
}
