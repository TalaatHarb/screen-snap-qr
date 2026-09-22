package net.talaatharb.screensnapqr.ui.viewer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collection;

import org.fxmisc.richtext.model.StyleSpans;
import org.junit.jupiter.api.Test;

class SyntaxHighlighterTest {

    @Test
    void testPlainTextHighlighter() {
        PlainTextHighlighter highlighter = new PlainTextHighlighter();

        StyleSpans<Collection<String>> emptySpans = highlighter.computeHighlighting("");
        assertEquals(0, emptySpans.length());

        StyleSpans<Collection<String>> nullSpans = highlighter.computeHighlighting(null);
        assertEquals(0, nullSpans.length());

        StyleSpans<Collection<String>> spans = highlighter.computeHighlighting("plain text here");
        assertEquals(15, spans.length());
        assertTrue(spans.getStyleSpan(0).getStyle().contains("token-text"));
    }

    @Test
    void testJsonSyntaxHighlighter() {
        JsonSyntaxHighlighter highlighter = new JsonSyntaxHighlighter();

        StyleSpans<Collection<String>> emptySpans = highlighter.computeHighlighting("");
        assertEquals(0, emptySpans.length());

        String json = "{\"key\": 123, \"flag\": true, \"other\": null}";
        StyleSpans<Collection<String>> spans = highlighter.computeHighlighting(json);
        assertEquals(json.length(), spans.length());
        assertNotNull(spans);
    }

    @Test
    void testXmlSyntaxHighlighter() {
        new XMLSyntaxHighlighter();

        StyleSpans<Collection<String>> emptySpans = XMLSyntaxHighlighter.computeHighlighting("");
        assertEquals(0, emptySpans.length());

        StyleSpans<Collection<String>> nullSpans = XMLSyntaxHighlighter.computeHighlighting(null);
        assertEquals(0, nullSpans.length());

        String xml = "<!-- comment --><![CDATA[cdata]]><root attr=\"value\">text</root>";
        StyleSpans<Collection<String>> spans = XMLSyntaxHighlighter.computeHighlighting(xml);
        assertEquals(xml.length(), spans.length());
        assertNotNull(spans);
    }

    @Test
    void testXmlSyntaxHighlighterSeparatesAttributeValuesFromTag() {
        String xml = "<bean id=\"foo\" class=\"com.example.Foo\"/>";

        StyleSpans<Collection<String>> spans = XMLSyntaxHighlighter.computeHighlighting(xml);

        assertEquals(xml.length(), spans.length());
        // Ensure at least one span is styled as "tag" and at least one as "string",
        // i.e. the whole tag (including attribute values) is not a single "tag" span.
        boolean hasTagSpan = false;
        boolean hasStringSpan = false;
        for (var span : spans) {
            if (span.getStyle().contains("tag")) {
                hasTagSpan = true;
            }
            if (span.getStyle().contains("string")) {
                hasStringSpan = true;
            }
        }
        assertTrue(hasTagSpan, "Expected at least one 'tag' styled span");
        assertTrue(hasStringSpan, "Expected at least one 'string' styled span for attribute values");
        assertTrue(spans.getSpanCount() > 1, "Expected multiple spans instead of one span for the whole tag");
    }

    @Test
    void testSyntaxHighlighterRegistry() {
        SyntaxHighlighter xmlHighlighter = SyntaxHighlighterRegistry.getHighlighterFor("test.xml");
        assertNotNull(xmlHighlighter);
        StyleSpans<Collection<String>> xmlSpans = xmlHighlighter.computeHighlighting("<root></root>");
        assertEquals(13, xmlSpans.length());

        SyntaxHighlighter jsonHighlighter = SyntaxHighlighterRegistry.getHighlighterFor("folder/data.json");
        assertTrue(jsonHighlighter instanceof JsonSyntaxHighlighter);

        SyntaxHighlighter defaultHighlighter = SyntaxHighlighterRegistry.getHighlighterFor("file.unknown");
        assertTrue(defaultHighlighter instanceof PlainTextHighlighter);

        SyntaxHighlighter nullHighlighter = SyntaxHighlighterRegistry.getHighlighterFor(null);
        assertTrue(nullHighlighter instanceof PlainTextHighlighter);

        SyntaxHighlighter blankHighlighter = SyntaxHighlighterRegistry.getHighlighterFor("   ");
        assertTrue(blankHighlighter instanceof PlainTextHighlighter);

        SyntaxHighlighter customHighlighter = text -> null;
        SyntaxHighlighterRegistry.register("custom", customHighlighter);
        assertEquals(customHighlighter, SyntaxHighlighterRegistry.getHighlighterFor("test.custom"));
    }
}
