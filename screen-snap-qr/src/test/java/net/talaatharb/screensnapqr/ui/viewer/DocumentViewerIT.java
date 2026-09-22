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

    @Test
    void testCrlfLineEndingsDoNotMisalignSyntaxHighlighting() {
        // Jackson's DefaultPrettyPrinter writes System.lineSeparator() (\r\n on Windows). RichTextFX's
        // CodeArea stores a single '\n' per line break internally, so without normalization the
        // \r\n-based content would be one character longer per line than what the CodeArea actually
        // stores, progressively shifting every style span after the first line break.
        final String crlfJson = "{\r\n  \"first\" : \"alpha\",\r\n  \"second\" : \"beta\",\r\n  \"third\" : \"gamma\"\r\n}";

        final AtomicReference<DocumentViewer> viewerRef = new AtomicReference<>();
        interact(() -> viewerRef.set(new DocumentViewer("sample.json", crlfJson)));

        final DocumentViewer viewer = viewerRef.get();
        // Line endings must be normalized so the CodeArea's actual text length matches the
        // string used to compute the highlighting offsets.
        assertEquals(viewer.getContent().length(), viewer.getCodeArea().getLength());
        assertFalse(viewer.getContent().contains("\r"));

        final String normalizedContent = viewer.getContent();
        final int thirdKeyStart = normalizedContent.indexOf("\"third\"");
        final var styles = viewer.getCodeArea().getStyleSpans(thirdKeyStart, thirdKeyStart + "\"third\"".length())
                .styleStream().toList();

        assertTrue(styles.stream().allMatch(style -> style.contains("token-string")),
                "Expected the 'third' key (on the last line) to be highlighted as a string, "
                        + "not drift onto an unrelated character due to CRLF offset misalignment");
    }
}
