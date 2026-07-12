package net.talaatharb.screensnapqr.ui.capture;

import java.awt.Rectangle;

import net.talaatharb.screensnapqr.constants.ModeChoice;

public interface CaptureBoundsProvider {

	Rectangle resolveBounds(ModeChoice modeChoice) throws Exception;
}
