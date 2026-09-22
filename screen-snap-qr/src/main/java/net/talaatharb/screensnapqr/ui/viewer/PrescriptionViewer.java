package net.talaatharb.screensnapqr.ui.viewer;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.image.Image;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import lombok.Getter;
import net.talaatharb.screensnapqr.JavafxApplication;
import net.talaatharb.screensnapqr.ui.prescription.PrescriptionDocument;
import net.talaatharb.screensnapqr.ui.prescription.PrescriptionView;

/**
 * Standalone, resizable window showing the human-readable visualization of a
 * detected Romanian e-prescription document, opened from the ZIP file tree
 * context menu (analogous to {@link DocumentViewer}).
 */
public class PrescriptionViewer {

    private static final int DEFAULT_WIDTH = 800;
    private static final int DEFAULT_HEIGHT = 600;

    @Getter
    private final String fileName;
    @Getter
    private final PrescriptionDocument document;
    @Getter
    private final VBox root;

    public PrescriptionViewer(String fileName, PrescriptionDocument document) {
        this.fileName = fileName != null ? fileName : "Prescription";
        this.document = document;
        this.root = new VBox();

        initializeUi();
    }

    private void initializeUi() {
        final HBox toolbar = buildToolbar();

        final Node prescriptionNode = PrescriptionView.build(document);
        VBox.setVgrow(prescriptionNode, Priority.ALWAYS);
        root.setSpacing(6);
        root.setPadding(new Insets(8));
        root.getChildren().addAll(toolbar, new Separator(), prescriptionNode);
    }

    private HBox buildToolbar() {
        final HBox toolbar = new HBox(8);
        toolbar.setAlignment(Pos.CENTER_LEFT);

        final Label fileLabel = new Label(fileName);
        fileLabel.getStyleClass().add("badge-format");

        final Label typeLabel = new Label(document.isOnline() ? "Online Prescription (<P>)" : "Offline Prescription (<O>)");
        typeLabel.getStyleClass().add("meta-label");

        toolbar.getChildren().addAll(fileLabel, typeLabel);
        return toolbar;
    }

    public Stage createStage() {
        final Stage stage = new Stage();
        stage.setTitle("Prescription Viewer - " + fileName);
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

    public static Stage show(String fileName, PrescriptionDocument document) {
        final PrescriptionViewer viewer = new PrescriptionViewer(fileName, document);
        final Stage stage = viewer.createStage();
        stage.show();
        return stage;
    }
}
