package net.talaatharb.screensnapqr.service;

import java.awt.Rectangle;
import java.awt.image.BufferedImage;

public interface ScreenSnapService {

	BufferedImage takeSnapshot() throws Exception;
	BufferedImage takeSnapshot(Rectangle captureBounds) throws Exception;

}
