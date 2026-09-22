package net.talaatharb.screensnapqr.ui.viewer;

import java.util.Collection;

import org.fxmisc.richtext.model.StyleSpans;

@FunctionalInterface
public interface SyntaxHighlighter {

    StyleSpans<Collection<String>> computeHighlighting(String text);
}
