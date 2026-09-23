package net.talaatharb.screensnapqr.ui.capture;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.awt.Rectangle;
import java.util.Locale;

import org.junit.jupiter.api.Test;

class ActiveWindowBoundsResolverTest {

    @Test
    void testGetFocusedWindowBoundsReturnsNonEmptyRectangleOnWindows() {
        assumeTrue(isWindows(), "Focused window bounds resolution is only supported on Windows.");

        ActiveWindowBoundsResolver resolver = new ActiveWindowBoundsResolver();
        Rectangle bounds = resolver.getFocusedWindowBounds();

        assertNotNull(bounds);
        assertTrue(bounds.width >= 0);
        assertTrue(bounds.height >= 0);
    }

    private boolean isWindows() {
        return System.getProperty("os.name", "").toLowerCase(Locale.ROOT).contains("win");
    }
}
