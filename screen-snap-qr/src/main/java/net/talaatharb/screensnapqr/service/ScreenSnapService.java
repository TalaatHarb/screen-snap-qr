package net.talaatharb.screensnapqr.service;

import java.awt.image.BufferedImage;

public interface ScreenSnapService {

	BufferedImage takeSnapshot() throws Exception;

}
