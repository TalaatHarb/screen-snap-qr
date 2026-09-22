package net.talaatharb.screensnapqr.ui.viewer;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.image.Image;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.FileChooser.ExtensionFilter;
import javafx.stage.Stage;
import javafx.stage.Window;
import lombok.Getter;
import net.talaatharb.screensnapqr.JavafxApplication;
import net.talaatharb.screensnapqr.ui.shc.ShcDocument;
import net.talaatharb.screensnapqr.ui.shc.ShcExporter;
import net.talaatharb.screensnapqr.ui.shc.ShcView;

/**
 * Standalone, resizable window showing the human-readable visualization of a
 * detected SMART Health Card (SHC) document, opened from the ZIP file tree or
 * Rendered tab context menu (analogous to {@link PrescriptionViewer}).
 */
public class ShcViewer {

    private static final int DEFAULT_WIDTH = 800;
    private static final int DEFAULT_HEIGHT = 600;

    private static final ExtensionFilter TEXT_FILTER = new ExtensionFilter("Text Report (*.txt)", "*.txt");
    private static final ExtensionFilter JSON_FILTER = new ExtensionFilter("JSON (*.json)", "*.json");

    @Getter
    private final String fileName;
    @Getter
    private final ShcDocument document;
    @Getter
    private final VBox root;

    public ShcViewer(String fileName, ShcDocument document) {
        this.fileName = fileName != null ? fileName : "Health Card";
        this.document = document;
        this.root = new VBox();

        initializeUi();
    }

    private void initializeUi() {
        final HBox toolbar = buildToolbar();

        final Node shcNode = ShcView.build(document);
        VBox.setVgrow(shcNode, Priority.ALWAYS);
        root.setSpacing(6);
        root.setPadding(new Insets(8));
        root.getChildren().addAll(toolbar, new Separator(), shcNode);
    }

    private HBox buildToolbar() {
        final HBox toolbar = new HBox(8);
        toolbar.setAlignment(Pos.CENTER_LEFT);

        final Label fileLabel = new Label(fileName);
        fileLabel.getStyleClass().add("badge-format");

        final Label typeLabel = new Label("SMART Health Card");
        typeLabel.getStyleClass().add("meta-label");

        final Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        final Button copyButton = new Button("Copy all details");
        copyButton.setOnAction(event -> copyAllDetails());

        final Button exportButton = new Button("Export...");
        exportButton.setOnAction(event -> export(exportButton.getScene() != null ? exportButton.getScene().getWindow() : null));

        toolbar.getChildren().addAll(fileLabel, typeLabel, spacer, copyButton, exportButton);
        return toolbar;
    }

    private void copyAllDetails() {
        final ClipboardContent clipboardContent = new ClipboardContent();
        clipboardContent.putString(ShcExporter.toPlainText(document));
        Clipboard.getSystemClipboard().setContent(clipboardContent);
    }

    private void export(Window ownerWindow) {
        final FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Export Health Card");
        fileChooser.setInitialFileName(baseFileName() + ".txt");
        fileChooser.getExtensionFilters().addAll(TEXT_FILTER, JSON_FILTER);

        final java.io.File targetFile = fileChooser.showSaveDialog(ownerWindow);
        if (targetFile == null) {
            return;
        }

        try {
            final boolean asJson = targetFile.getName().toLowerCase().endsWith(".json")
                    || fileChooser.getSelectedExtensionFilter() == JSON_FILTER;
            final String content = asJson ? ShcExporter.toJson(document) : ShcExporter.toPlainText(document);
            Files.writeString(targetFile.toPath(), content, StandardCharsets.UTF_8);
        } catch (IOException ex) {
            showExportError(ex);
        }
    }

    private void showExportError(Exception ex) {
        final Alert alert = new Alert(AlertType.ERROR);
        alert.setTitle("Export Failed");
        alert.setHeaderText("Could not export health card data");
        alert.setContentText(ex.getMessage());
        alert.showAndWait();
    }

    private String baseFileName() {
        final int dotIndex = fileName.lastIndexOf('.');
        final String base = dotIndex > 0 ? fileName.substring(0, dotIndex) : fileName;
        final int slashIndex = Math.max(base.lastIndexOf('/'), base.lastIndexOf('\\'));
        return slashIndex >= 0 ? base.substring(slashIndex + 1) : base;
    }

    public Stage createStage() {
        final Stage stage = new Stage();
        stage.setTitle("Health Card Viewer - " + fileName);
        stage.setResizable(true);

        final Scene scene = new Scene(root, DEFAULT_WIDTH, DEFAULT_HEIGHT);
        try {
            var cssUrl = JavafxApplication.class.getResource(JavafxApplication.CSS_FILE);
            if (cssUrl != null) {
                scene.getStylesheets().add(cssUrl.toExternalForm());
            }
        } catch (Exception ignored) {
        }

        try {
            var iconStream = JavafxApplication.class.getResourceAsStream(JavafxApplication.ICON_FILE);
            if (iconStream != null) {
                stage.getIcons().add(new Image(iconStream));
            }
        } catch (Exception ignored) {
        }

        stage.setScene(scene);
        return stage;
    }

    public static Stage show(String fileName, ShcDocument document) {
        final ShcViewer viewer = new ShcViewer(fileName, document);
        final Stage stage = viewer.createStage();
        stage.show();
        return stage;
    }
}
