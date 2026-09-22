package net.talaatharb.screensnapqr.ui.viewer;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class SyntaxHighlighterRegistry {

    private static final Map<String, SyntaxHighlighter> HIGHLIGHTERS = new ConcurrentHashMap<>();
    private static final SyntaxHighlighter DEFAULT_HIGHLIGHTER = new PlainTextHighlighter();

    static {
        final SyntaxHighlighter xmlHighlighter = XMLSyntaxHighlighter::computeHighlighting;
        final JsonSyntaxHighlighter jsonHighlighter = new JsonSyntaxHighlighter();

        register("xml", xmlHighlighter);
        register("html", xmlHighlighter);
        register("xhtml", xmlHighlighter);
        register("svg", xmlHighlighter);
        register("json", jsonHighlighter);
        register("txt", DEFAULT_HIGHLIGHTER);
    }

    public static void register(String extension, SyntaxHighlighter highlighter) {
        if (extension != null && highlighter != null) {
            HIGHLIGHTERS.put(extension.toLowerCase(), highlighter);
        }
    }

    public static SyntaxHighlighter getHighlighterFor(String fileName) {
        if (fileName == null || fileName.isBlank()) {
            return DEFAULT_HIGHLIGHTER;
        }

        final int dotIndex = fileName.lastIndexOf('.');
        if (dotIndex >= 0 && dotIndex < fileName.length() - 1) {
            final String ext = fileName.substring(dotIndex + 1).toLowerCase();
            return HIGHLIGHTERS.getOrDefault(ext, DEFAULT_HIGHLIGHTER);
        }

        return DEFAULT_HIGHLIGHTER;
    }
}
