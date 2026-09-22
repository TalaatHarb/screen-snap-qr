package net.talaatharb.screensnapqr.ui.viewer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;
import org.testfx.framework.junit5.ApplicationTest;

import javafx.stage.Stage;
import net.talaatharb.screensnapqr.ui.prescription.PrescriptionDocument;

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
}
