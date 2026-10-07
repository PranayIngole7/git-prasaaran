package com.pranay.gitprasaaran.infrastructure.markdown;

import com.pranay.gitprasaaran.domain.document.Document;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class MarkdownDocumentParserTest {

    private final MarkdownDocumentParser parser =
            new MarkdownDocumentParser();

    @Test
    void shouldRenderMarkdownToHtml() {
        Document document = parser.parse(
                "hello",
                "# Hello\n\nThis is **Markdown**.",
                "docs/hello.md"
        );

        assertEquals("hello", document.slug());
        assertEquals("<h1>Hello</h1>\n<p>This is <strong>Markdown</strong>.</p>\n",
                document.html());
    }

    @Test
    void shouldRemoveUnsafeHtml() {
        Document document = parser.parse(
                "unsafe",
                "# Hello\n\n<script>alert('xss')</script>\n\n" +
                        "<img src=\"x\" onerror=\"alert('xss')\">",
                "docs/unsafe.md"
        );

        assertFalse(document.html().contains("<script>"));
        assertFalse(document.html().contains("onerror"));
        assertFalse(document.html().contains("alert('xss')"));
        assertTrue(document.html().contains("<h1>Hello</h1>"));
    }

    @Test
    void shouldAllowSafeLinks() {
        Document document = parser.parse(
                "links",
                "[GitHub](https://github.com)",
                "docs/links.md"
        );

        assertTrue(document.html().contains("<a"));
        assertTrue(document.html().contains("https://github.com"));
    }

    @Test
    void shouldRemoveJavascriptLinks() {
        Document document = parser.parse(
                "bad-link",
                "[Click](javascript:alert('xss'))",
                "docs/bad-link.md"
        );

        assertFalse(document.html().contains("javascript:"));
        assertFalse(document.html().contains("alert"));
    }
}
