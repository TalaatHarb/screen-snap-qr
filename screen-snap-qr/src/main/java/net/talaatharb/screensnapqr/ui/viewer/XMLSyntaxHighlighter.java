package net.talaatharb.screensnapqr.ui.viewer;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.fxmisc.richtext.model.StyleSpans;
import org.fxmisc.richtext.model.StyleSpansBuilder;

public class XMLSyntaxHighlighter {

    private static final Pattern ATTRIBUTE_VALUE_PATTERN = Pattern.compile("\"[^\"]*\"|'[^']*'");

    private static class Token {
        int start;
        int end;
        String style;

        Token(int start, int end, String style) {
            this.start = start;
            this.end = end;
            this.style = style;
        }
    }

    public static StyleSpans<Collection<String>> computeHighlighting(String text) {
        if (text == null || text.isEmpty()) {
            return new StyleSpansBuilder<Collection<String>>().add(Collections.emptyList(), 0).create();
        }

        List<Token> tokens = new LinkedList<>();

        // Match: comments, CDATA, tags, and strings
        Pattern pattern = Pattern.compile(
            "<!--.*?-->|<\\!\\[CDATA\\[.*?\\]\\]>|<[^>]+>|\"[^\"]*\"|'[^']*'",
            Pattern.DOTALL
        );

        Matcher matcher = pattern.matcher(text);
        while (matcher.find()) {
            String match = matcher.group();

            if (match.startsWith("<!--")) {
                tokens.add(new Token(matcher.start(), matcher.end(), "comment"));
            } else if (match.startsWith("<![CDATA[")) {
                tokens.add(new Token(matcher.start(), matcher.end(), "string"));
            } else if (match.startsWith("<")) {
                // Split the tag so quoted attribute values get their own "string" style
                // instead of the whole tag (including attributes) being one "tag" span.
                tokens.addAll(tokenizeTag(matcher.start(), match));
            } else {
                tokens.add(new Token(matcher.start(), matcher.end(), "string"));
            }
        }

        // Build spans
        StyleSpansBuilder<Collection<String>> spansBuilder = new StyleSpansBuilder<>();
        int lastPos = 0;

        for (Token token : tokens) {
            if (token.start > lastPos) {
                spansBuilder.add(Collections.emptyList(), token.start - lastPos);
            }
            spansBuilder.add(Collections.singleton(token.style), token.end - token.start);
            lastPos = token.end;
        }

        if (lastPos < text.length()) {
            spansBuilder.add(Collections.emptyList(), text.length() - lastPos);
        }

        return spansBuilder.create();
    }

    private static List<Token> tokenizeTag(int tagStart, String tagText) {
        final List<Token> tagTokens = new LinkedList<>();
        final Matcher attrMatcher = ATTRIBUTE_VALUE_PATTERN.matcher(tagText);
        int lastEnd = 0;

        while (attrMatcher.find()) {
            if (attrMatcher.start() > lastEnd) {
                tagTokens.add(new Token(tagStart + lastEnd, tagStart + attrMatcher.start(), "tag"));
            }
            tagTokens.add(new Token(tagStart + attrMatcher.start(), tagStart + attrMatcher.end(), "string"));
            lastEnd = attrMatcher.end();
        }

        if (lastEnd < tagText.length()) {
            tagTokens.add(new Token(tagStart + lastEnd, tagStart + tagText.length(), "tag"));
        }

        return tagTokens;
    }
}
