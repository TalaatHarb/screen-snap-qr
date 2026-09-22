package net.talaatharb.screensnapqr.ui.viewer;

import org.fxmisc.flowless.VirtualizedScrollPane;
import org.fxmisc.richtext.CodeArea;
import org.fxmisc.richtext.LineNumberFactory;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
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
import javafx.stage.Stage;
import lombok.Getter;
import net.talaatharb.screensnapqr.JavafxApplication;

public class DocumentViewer {

    private static final int DEFAULT_WIDTH = 700;
    private static final int DEFAULT_HEIGHT = 500;

    @Getter
    private final String fileName;
    @Getter
    private final String content;
    @Getter
    private final CodeArea codeArea;
    @Getter
    private final VirtualizedScrollPane<CodeArea> scrollPane;
    @Getter
    private final VBox root;

    public DocumentViewer(String fileName, String content) {
        this.fileName = fileName != null ? fileName : "Untitled";
        // RichTextFX's CodeArea splits paragraphs on \r\n|\r|\n and stores a single '\n'
        // per line break internally, so a \r\n-based string (e.g. Jackson's pretty printer
        // on Windows, which uses System.lineSeparator()) would end up one character shorter
        // per line inside the CodeArea than the string used to compute highlighting offsets,
        // progressively misaligning every style span after the first line break.
        this.content = normalizeLineEndings(content != null ? content : "");
        this.codeArea = new CodeArea();
        this.scrollPane = new VirtualizedScrollPane<>(codeArea);
        this.root = new VBox();

        initializeUi();
    }

    private static String normalizeLineEndings(String text) {
        return text.replace("\r\n", "\n").replace("\r", "\n");
    }

    private void initializeUi() {
        codeArea.setEditable(false);
        codeArea.setParagraphGraphicFactory(LineNumberFactory.get(codeArea));
        codeArea.getStyleClass().add("code-area");

        codeArea.replaceText(content);
        if (!content.isEmpty()) {
            final SyntaxHighlighter highlighter = SyntaxHighlighterRegistry.getHighlighterFor(fileName);
            codeArea.setStyleSpans(0, highlighter.computeHighlighting(content));
        }

        final HBox toolbar = buildToolbar();

        VBox.setVgrow(scrollPane, Priority.ALWAYS);
        root.setSpacing(6);
        root.setPadding(new Insets(8));
        root.getChildren().addAll(toolbar, new Separator(), scrollPane);
    }

    private HBox buildToolbar() {
        final HBox toolbar = new HBox(8);
        toolbar.setAlignment(Pos.CENTER_LEFT);

        final Label fileLabel = new Label(fileName);
        fileLabel.getStyleClass().add("badge-format");

        final int lineCount = content.isEmpty() ? 0 : content.split("\\R", -1).length;
        final Label infoLabel = new Label(lineCount + " lines, " + content.length() + " chars");
        infoLabel.getStyleClass().add("meta-label");

        final Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        final Button copyButton = new Button("Copy content");
        copyButton.setOnAction(e -> {
            final ClipboardContent clipboardContent = new ClipboardContent();
            clipboardContent.putString(content);
            Clipboard.getSystemClipboard().setContent(clipboardContent);
        });

        toolbar.getChildren().addAll(fileLabel, infoLabel, spacer, copyButton);
        return toolbar;
    }

    public Stage createStage() {
        final Stage stage = new Stage();
        stage.setTitle("Document Viewer - " + fileName);

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

    public static Stage show(String fileName, String content) {
        final DocumentViewer viewer = new DocumentViewer(fileName, content);
        final Stage stage = viewer.createStage();
        stage.show();
        return stage;
    }
}
