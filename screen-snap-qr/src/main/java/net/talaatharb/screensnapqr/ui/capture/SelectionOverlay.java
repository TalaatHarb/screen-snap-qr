package net.talaatharb.screensnapqr.ui.capture;

import java.awt.Rectangle;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicReference;

import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.geometry.BoundingBox;
import javafx.geometry.Bounds;
import javafx.geometry.Point2D;
import javafx.scene.Cursor;
import javafx.scene.Scene;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;

public class SelectionOverlay {

	public Rectangle requestSelectionBounds() throws Exception {
		CompletableFuture<Rectangle> resultFuture = new CompletableFuture<>();
		runOnFxThread(() -> showSelectionOverlay(resultFuture));

		try {
			return resultFuture.get();
		} catch (ExecutionException e) {
			Throwable cause = e.getCause();
			if (cause instanceof Exception ex) {
				throw ex;
			}
			throw new IllegalStateException("Selection overlay failed.", cause);
		}
	}

	public void flashBounds(Rectangle bounds) {
		if (bounds == null || bounds.width <= 0 || bounds.height <= 0) {
			return;
		}

		runOnFxThread(() -> {
			Stage stage = new Stage(StageStyle.TRANSPARENT);
			stage.setAlwaysOnTop(true);
			stage.setX(bounds.x);
			stage.setY(bounds.y);

			javafx.scene.shape.Rectangle highlight = new javafx.scene.shape.Rectangle(bounds.width, bounds.height);
			highlight.setStroke(Color.web("#4AA3FF"));
			highlight.setFill(Color.web("#4AA3FF55"));
			highlight.setStrokeWidth(2);

			Pane pane = new Pane(highlight);
			Scene scene = new Scene(pane, bounds.width, bounds.height, Color.TRANSPARENT);
			stage.setScene(scene);
			stage.show();

			PauseTransition pause = new PauseTransition(Duration.millis(250));
			pause.setOnFinished(e -> stage.close());
			pause.play();
		});
	}

	private void showSelectionOverlay(CompletableFuture<Rectangle> resultFuture) {
		Bounds virtualBounds = getVirtualScreenBounds();
		Stage stage = new Stage(StageStyle.TRANSPARENT);
		stage.setAlwaysOnTop(true);
		stage.setOnHidden(event -> resultFuture.complete(null));
		stage.setX(virtualBounds.getMinX());
		stage.setY(virtualBounds.getMinY());
		stage.setWidth(virtualBounds.getWidth());
		stage.setHeight(virtualBounds.getHeight());

		Pane root = new Pane();
		root.setStyle("-fx-background-color: rgba(0, 0, 0, 0.35);");
		root.setCursor(Cursor.CROSSHAIR);

		javafx.scene.shape.Rectangle selected = new javafx.scene.shape.Rectangle();
		selected.setVisible(false);
		selected.setStroke(Color.web("#4AA3FF"));
		selected.setFill(Color.web("#4AA3FF55"));
		selected.setStrokeWidth(2);
		root.getChildren().add(selected);

		AtomicReference<Point2D> anchor = new AtomicReference<>();

		root.addEventHandler(MouseEvent.MOUSE_PRESSED, event -> {
			anchor.set(new Point2D(event.getScreenX(), event.getScreenY()));
			selected.setVisible(true);
			selected.setX(event.getScreenX() - virtualBounds.getMinX());
			selected.setY(event.getScreenY() - virtualBounds.getMinY());
			selected.setWidth(0);
			selected.setHeight(0);
		});

		root.addEventHandler(MouseEvent.MOUSE_DRAGGED, event -> {
			var start = anchor.get();
			if (start == null) {
				return;
			}
			var minX = Math.min(start.getX(), event.getScreenX());
			var minY = Math.min(start.getY(), event.getScreenY());
			var width = Math.abs(event.getScreenX() - start.getX());
			var height = Math.abs(event.getScreenY() - start.getY());

			selected.setX(minX - virtualBounds.getMinX());
			selected.setY(minY - virtualBounds.getMinY());
			selected.setWidth(width);
			selected.setHeight(height);
		});

		root.addEventHandler(MouseEvent.MOUSE_RELEASED, event -> {
			if (!selected.isVisible() || selected.getWidth() < 2 || selected.getHeight() < 2) {
				resultFuture.complete(null);
				stage.close();
				return;
			}
			Rectangle bounds = new Rectangle(
					(int) Math.round(selected.getX() + virtualBounds.getMinX()),
					(int) Math.round(selected.getY() + virtualBounds.getMinY()),
					(int) Math.round(selected.getWidth()),
					(int) Math.round(selected.getHeight()));
			resultFuture.complete(toDeviceBounds(bounds));
			stage.close();
		});

		Scene scene = new Scene(root, virtualBounds.getWidth(), virtualBounds.getHeight(), Color.TRANSPARENT);
		scene.setOnKeyPressed(event -> {
			if (event.getCode() == KeyCode.ESCAPE) {
				resultFuture.complete(null);
				stage.close();
			}
		});
		stage.setScene(scene);
		stage.show();
		scene.getRoot().requestFocus();
	}

	private Bounds getVirtualScreenBounds() {
		double minX = Screen.getScreens().stream().mapToDouble(s -> s.getBounds().getMinX()).min().orElse(0);
		double minY = Screen.getScreens().stream().mapToDouble(s -> s.getBounds().getMinY()).min().orElse(0);
		double maxX = Screen.getScreens().stream().mapToDouble(s -> s.getBounds().getMaxX()).max().orElse(0);
		double maxY = Screen.getScreens().stream().mapToDouble(s -> s.getBounds().getMaxY()).max().orElse(0);
		return new BoundingBox(minX, minY, maxX - minX, maxY - minY);
	}

	private void runOnFxThread(Runnable runnable) {
		if (Platform.isFxApplicationThread()) {
			runnable.run();
			return;
		}
		CompletableFuture<Void> fxFuture = new CompletableFuture<>();
		Platform.runLater(() -> {
			try {
				runnable.run();
				fxFuture.complete(null);
			} catch (Exception e) {
				fxFuture.completeExceptionally(e);
			}
		});
		try {
			fxFuture.get();
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new IllegalStateException("Interrupted while waiting for FX thread.", e);
		} catch (ExecutionException e) {
			throw new IllegalStateException("FX execution failed.", e.getCause());
		}
	}

	private Rectangle toDeviceBounds(Rectangle logicalBounds) {
		double centerX = logicalBounds.getCenterX();
		double centerY = logicalBounds.getCenterY();
		Screen screen = Screen.getScreens().stream()
				.filter(s -> s.getBounds().contains(centerX, centerY))
				.findFirst()
				.orElse(Screen.getPrimary());

		double scaleX = screen.getOutputScaleX();
		double scaleY = screen.getOutputScaleY();

		int left = (int) Math.round(logicalBounds.getMinX() * scaleX);
		int top = (int) Math.round(logicalBounds.getMinY() * scaleY);
		int right = (int) Math.round(logicalBounds.getMaxX() * scaleX);
		int bottom = (int) Math.round(logicalBounds.getMaxY() * scaleY);

		return new Rectangle(left, top, Math.max(1, right - left), Math.max(1, bottom - top));
	}
}
