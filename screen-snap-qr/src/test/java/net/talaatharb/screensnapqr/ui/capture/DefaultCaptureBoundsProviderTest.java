package net.talaatharb.screensnapqr.ui.capture;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.awt.Rectangle;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import net.talaatharb.screensnapqr.constants.ModeChoice;

@ExtendWith(MockitoExtension.class)
class DefaultCaptureBoundsProviderTest {

    @Mock
    SelectionOverlay selectionOverlay;

    @Mock
    ActiveWindowBoundsResolver activeWindowBoundsResolver;

    @Test
    void testNullModeChoiceReturnsNull() throws Exception {
        DefaultCaptureBoundsProvider provider = new DefaultCaptureBoundsProvider(selectionOverlay,
                activeWindowBoundsResolver);

        assertNull(provider.resolveBounds(null));
        verify(selectionOverlay, never()).requestSelectionBounds();
        verify(activeWindowBoundsResolver, never()).getFocusedWindowBounds();
    }

    @Test
    void testScreenModeReturnsNull() throws Exception {
        DefaultCaptureBoundsProvider provider = new DefaultCaptureBoundsProvider(selectionOverlay,
                activeWindowBoundsResolver);

        assertNull(provider.resolveBounds(ModeChoice.SCREEN));
        verify(selectionOverlay, never()).requestSelectionBounds();
        verify(activeWindowBoundsResolver, never()).getFocusedWindowBounds();
    }

    @Test
    void testSelectionModeDelegatesToSelectionOverlay() throws Exception {
        Rectangle expected = new Rectangle(1, 2, 3, 4);
        when(selectionOverlay.requestSelectionBounds()).thenReturn(expected);
        DefaultCaptureBoundsProvider provider = new DefaultCaptureBoundsProvider(selectionOverlay,
                activeWindowBoundsResolver);

        assertSame(expected, provider.resolveBounds(ModeChoice.SELECTION));
        verify(activeWindowBoundsResolver, never()).getFocusedWindowBounds();
    }

    @Test
    void testWindowModeDelegatesToActiveWindowResolverAndFlashesBounds() throws Exception {
        Rectangle expected = new Rectangle(5, 6, 7, 8);
        when(activeWindowBoundsResolver.getFocusedWindowBounds()).thenReturn(expected);
        DefaultCaptureBoundsProvider provider = new DefaultCaptureBoundsProvider(selectionOverlay,
                activeWindowBoundsResolver);

        assertSame(expected, provider.resolveBounds(ModeChoice.WINDOW));
        verify(selectionOverlay).flashBounds(expected);
        verify(selectionOverlay, never()).requestSelectionBounds();
    }
}
