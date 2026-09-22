package net.talaatharb.screensnapqr.ui.viewer;

import java.util.Collection;
import java.util.Collections;

import org.fxmisc.richtext.model.StyleSpans;
import org.fxmisc.richtext.model.StyleSpansBuilder;

public class PlainTextHighlighter implements SyntaxHighlighter {

    @Override
    public StyleSpans<Collection<String>> computeHighlighting(String text) {
        final StyleSpansBuilder<Collection<String>> spansBuilder = new StyleSpansBuilder<>();
        if (text == null || text.isEmpty()) {
            spansBuilder.add(Collections.emptyList(), 0);
        } else {
            spansBuilder.add(Collections.singleton("token-text"), text.length());
        }
        return spansBuilder.create();
    }
}
