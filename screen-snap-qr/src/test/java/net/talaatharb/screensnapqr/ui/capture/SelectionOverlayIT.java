package net.talaatharb.screensnapqr.ui.capture;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Rectangle;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;
import org.testfx.framework.junit5.ApplicationTest;

import javafx.geometry.Point2D;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseButton;
import javafx.stage.Screen;
import javafx.stage.Stage;

class SelectionOverlayIT extends ApplicationTest {

    @Test
    void testFlashBoundsWithEmptyBoundsIsNoOp() {
        SelectionOverlay overlay = new SelectionOverlay();

        // Guard clauses should return immediately without touching the FX thread.
        overlay.flashBounds(null);
        overlay.flashBounds(new Rectangle(0, 0, 0, 0));
        overlay.flashBounds(new Rectangle(0, 0, -5, 10));
    }

    @Test
    void testFlashBoundsShowsAndAutoClosesHighlightStage() {
        SelectionOverlay overlay = new SelectionOverlay();
        Rectangle bounds = new Rectangle(10, 10, 100, 80);
        int windowCountBefore = Stage.getWindows().size();

        interact(() -> overlay.flashBounds(bounds));

        // The highlight stage is shown immediately and auto-closes after a short pause.
        await().atMost(Duration.ofSeconds(1)).until(() -> Stage.getWindows().size() > windowCountBefore);
        await().atMost(Duration.ofSeconds(3)).until(() -> Stage.getWindows().size() == windowCountBefore);
    }

    @Test
    void testRequestSelectionBoundsCancelledWithEscapeReturnsNull() throws Exception {
        SelectionOverlay overlay = new SelectionOverlay();
        CompletableFuture<Rectangle> resultFuture = new CompletableFuture<>();

        Thread worker = new Thread(() -> {
            try {
                resultFuture.complete(overlay.requestSelectionBounds());
            } catch (Exception e) {
                resultFuture.completeExceptionally(e);
            }
        });
        worker.start();

        await().atMost(Duration.ofSeconds(3)).until(() -> Stage.getWindows().stream()
                .anyMatch(window -> window.getScene() != null && window.isShowing()
                        && window.getScene().getOnKeyPressed() != null));

        interact(() -> press(KeyCode.ESCAPE).release(KeyCode.ESCAPE));

        Rectangle result = resultFuture.get(5, java.util.concurrent.TimeUnit.SECONDS);
        assertNull(result);
        worker.join(2000);
    }

    @Test
    void testRequestSelectionBoundsWithDragReturnsDeviceBounds() throws Exception {
        SelectionOverlay overlay = new SelectionOverlay();
        AtomicReference<Rectangle> resultRef = new AtomicReference<>();
        CompletableFuture<Rectangle> resultFuture = new CompletableFuture<>();

        Thread worker = new Thread(() -> {
            try {
                resultFuture.complete(overlay.requestSelectionBounds());
            } catch (Exception e) {
                resultFuture.completeExceptionally(e);
            }
        });
        worker.start();

        await().atMost(Duration.ofSeconds(3)).until(() -> Stage.getWindows().stream()
                .anyMatch(window -> window.getScene() != null && window.isShowing()
                        && window.getScene().getOnKeyPressed() != null));

        Screen primary = Screen.getPrimary();
        double startX = primary.getVisualBounds().getMinX() + 50;
        double startY = primary.getVisualBounds().getMinY() + 50;
        double endX = startX + 120;
        double endY = startY + 90;

        interact(() -> {
            moveTo(new Point2D(startX, startY));
            press(MouseButton.PRIMARY);
            moveTo(new Point2D(endX, endY));
            release(MouseButton.PRIMARY);
        });

        Rectangle result = resultFuture.get(5, java.util.concurrent.TimeUnit.SECONDS);
        resultRef.set(result);
        worker.join(2000);

        assertNotNull(resultRef.get());
        assertTrue(resultRef.get().width > 0);
        assertTrue(resultRef.get().height > 0);
    }
}
