package net.talaatharb.screensnapqr.ui.viewer;

import java.util.Collection;
import java.util.Collections;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.fxmisc.richtext.model.StyleSpans;
import org.fxmisc.richtext.model.StyleSpansBuilder;

public class JsonSyntaxHighlighter implements SyntaxHighlighter {

    private static final Pattern JSON_PATTERN = Pattern.compile(
            "\"(?:\\\\.|[^\"\\\\])*\"|-?\\d+(?:\\.\\d+)?(?:[eE][+-]?\\d+)?|\\btrue\\b|\\bfalse\\b|\\bnull\\b|[\\[\\]{}:,]");

    @Override
    public StyleSpans<Collection<String>> computeHighlighting(String text) {
        if (text == null || text.isEmpty()) {
            final StyleSpansBuilder<Collection<String>> builder = new StyleSpansBuilder<>();
            builder.add(Collections.emptyList(), 0);
            return builder.create();
        }

        final Matcher matcher = JSON_PATTERN.matcher(text);
        int lastKwEnd = 0;
        final StyleSpansBuilder<Collection<String>> spansBuilder = new StyleSpansBuilder<>();

        while (matcher.find()) {
            final String styleClass;
            final String token = matcher.group();
            if (token.startsWith("\"")) {
                styleClass = "token-string";
            } else if (token.equals("true") || token.equals("false")) {
                styleClass = "token-boolean";
            } else if (token.equals("null")) {
                styleClass = "token-null";
            } else if ("[]{}:,".contains(token)) {
                styleClass = "token-punctuation";
            } else {
                styleClass = "token-number";
            }

            spansBuilder.add(Collections.singleton("token-text"), matcher.start() - lastKwEnd);
            spansBuilder.add(Collections.singleton(styleClass), matcher.end() - matcher.start());
            lastKwEnd = matcher.end();
        }

        spansBuilder.add(Collections.singleton("token-text"), text.length() - lastKwEnd);
        return spansBuilder.create();
    }
}
