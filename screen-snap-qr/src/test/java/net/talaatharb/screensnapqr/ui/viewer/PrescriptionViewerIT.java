package net.talaatharb.screensnapqr.ui.viewer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;
import org.testfx.framework.junit5.ApplicationTest;

import javafx.scene.control.Button;
import javafx.stage.Stage;
import net.talaatharb.screensnapqr.ui.prescription.PrescriptionDocument;
import net.talaatharb.screensnapqr.ui.prescription.PrescriptionExporter;

class PrescriptionViewerIT extends ApplicationTest {

    private static PrescriptionDocument sampleDocument() {
        return PrescriptionDocument.builder()
                .rootElementName("P")
                .headerAttributes(Map.of("SC", "AB", "SN", "1234567"))
                .prescriptionDetails(Map.of("FN", "Ion", "LN", "Popescu"))
                .build();
    }

    @Test
    void testPrescriptionViewerInitialization() {
        AtomicReference<PrescriptionViewer> viewerRef = new AtomicReference<>();
        AtomicReference<Stage> stageRef = new AtomicReference<>();

        interact(() -> {
            PrescriptionViewer viewer = new PrescriptionViewer("prescription.xml", sampleDocument());
            viewerRef.set(viewer);
            stageRef.set(viewer.createStage());
        });

        PrescriptionViewer viewer = viewerRef.get();
        assertNotNull(viewer);
        assertEquals("prescription.xml", viewer.getFileName());
        assertNotNull(viewer.getDocument());
        assertTrue(viewer.getDocument().isOnline());

        Stage stage = stageRef.get();
        assertNotNull(stage);
        assertEquals("Prescription Viewer - prescription.xml", stage.getTitle());
        assertTrue(stage.isResizable());
        assertNotNull(stage.getScene());
        assertFalse(stage.getScene().getStylesheets().isEmpty());
    }

    @Test
    void testPrescriptionViewerShow() {
        AtomicReference<Stage> stageRef = new AtomicReference<>();

        interact(() -> {
            Stage stage = PrescriptionViewer.show("data.xml", sampleDocument());
            stageRef.set(stage);
        });

        Stage stage = stageRef.get();
        assertNotNull(stage);
        assertTrue(stage.isShowing());
        interact(stage::close);
    }

    @Test
    void testCopyAllDetailsPlacesPlainTextReportOnClipboard() {
        AtomicReference<PrescriptionViewer> viewerRef = new AtomicReference<>();

        interact(() -> {
            PrescriptionViewer viewer = new PrescriptionViewer("prescription.xml", sampleDocument());
            viewerRef.set(viewer);
            viewer.createStage();

            Button copyButton = findButton(viewer.getRoot(), "Copy all details");
            assertNotNull(copyButton);
            copyButton.fire();
        });

        final String expected = PrescriptionExporter.toPlainText(sampleDocument());
        interact(() -> assertEquals(expected, javafx.scene.input.Clipboard.getSystemClipboard().getString()));
    }

    @Test
    void testToolbarExposesExportButton() {
        interact(() -> {
            PrescriptionViewer viewer = new PrescriptionViewer("prescription.xml", sampleDocument());
            viewer.createStage();
            assertNotNull(findButton(viewer.getRoot(), "Export..."));
        });
    }

    private static Button findButton(javafx.scene.Parent parent, String text) {
        for (var child : parent.getChildrenUnmodifiable()) {
            if (child instanceof Button button && text.equals(button.getText())) {
                return button;
            }
            if (child instanceof javafx.scene.Parent childParent) {
                Button found = findButton(childParent, text);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }
}
