package net.talaatharb.screensnapqr.ui.viewer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.testfx.framework.junit5.ApplicationTest;

import javafx.application.Platform;
import javafx.stage.Stage;

@ExtendWith(MockitoExtension.class)
class DocumentViewerIT extends ApplicationTest {

    @Test
    void testDocumentViewerInitialization() {
        AtomicReference<DocumentViewer> viewerRef = new AtomicReference<>();
        AtomicReference<Stage> stageRef = new AtomicReference<>();

        interact(() -> {
            DocumentViewer viewer = new DocumentViewer("sample.json", "{\"name\": \"test\"}");
            viewerRef.set(viewer);
            stageRef.set(viewer.createStage());
        });

        DocumentViewer viewer = viewerRef.get();
        assertNotNull(viewer);
        assertEquals("sample.json", viewer.getFileName());
        assertEquals("{\"name\": \"test\"}", viewer.getContent());
        assertFalse(viewer.getCodeArea().isEditable());
        assertEquals("{\"name\": \"test\"}", viewer.getCodeArea().getText());
        assertNotNull(viewer.getCodeArea().getParagraphGraphicFactory());

        Stage stage = stageRef.get();
        assertNotNull(stage);
        assertEquals("Document Viewer - sample.json", stage.getTitle());
        assertNotNull(stage.getScene());
        assertFalse(stage.getScene().getStylesheets().isEmpty());
    }

    @Test
    void testDocumentViewerShow() {
        AtomicReference<Stage> stageRef = new AtomicReference<>();

        interact(() -> {
            Stage stage = DocumentViewer.show("data.xml", "<root></root>");
            stageRef.set(stage);
        });

        Stage stage = stageRef.get();
        assertNotNull(stage);
        assertTrue(stage.isShowing());
        interact(stage::close);
    }
}
