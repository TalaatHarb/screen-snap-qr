package net.talaatharb.screensnapqr.ui.capture;

import java.awt.Rectangle;

import lombok.RequiredArgsConstructor;
import net.talaatharb.screensnapqr.constants.ModeChoice;

@RequiredArgsConstructor
public class DefaultCaptureBoundsProvider implements CaptureBoundsProvider {

	private final SelectionOverlay selectionOverlay;
	private final ActiveWindowBoundsResolver activeWindowBoundsResolver;

	@Override
	public Rectangle resolveBounds(ModeChoice modeChoice) throws Exception {
		if (modeChoice == null || modeChoice == ModeChoice.SCREEN) {
			return null;
		}
		if (modeChoice == ModeChoice.SELECTION) {
			return selectionOverlay.requestSelectionBounds();
		}
		Rectangle windowBounds = activeWindowBoundsResolver.getFocusedWindowBounds();
		selectionOverlay.flashBounds(windowBounds);
		return windowBounds;
	}
}
