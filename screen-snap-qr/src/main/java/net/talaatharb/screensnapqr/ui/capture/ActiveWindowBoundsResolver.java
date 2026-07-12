package net.talaatharb.screensnapqr.ui.capture;

import java.awt.Rectangle;
import java.util.Locale;

import com.sun.jna.platform.win32.User32;
import com.sun.jna.platform.win32.WinDef.HWND;
import com.sun.jna.platform.win32.WinDef.RECT;

public class ActiveWindowBoundsResolver {

	public Rectangle getFocusedWindowBounds() {
		if (!isWindows()) {
			throw new UnsupportedOperationException("Window capture mode is currently supported on Windows only.");
		}

		HWND handle = User32.INSTANCE.GetForegroundWindow();
		if (handle == null) {
			throw new IllegalStateException("Unable to resolve focused window handle.");
		}

		RECT rect = new RECT();
		if (!User32.INSTANCE.GetWindowRect(handle, rect)) {
			throw new IllegalStateException("Unable to resolve focused window bounds.");
		}

		return new Rectangle(rect.left, rect.top, rect.right - rect.left, rect.bottom - rect.top);
	}

	private boolean isWindows() {
		return System.getProperty("os.name", "").toLowerCase(Locale.ROOT).contains("win");
	}
}
