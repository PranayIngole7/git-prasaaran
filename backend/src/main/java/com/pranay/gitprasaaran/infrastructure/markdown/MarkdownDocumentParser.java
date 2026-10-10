package com.pranay.gitprasaaran.infrastructure.markdown;

import com.pranay.gitprasaaran.domain.document.Document;
import org.commonmark.node.Node;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.HtmlRenderer;
import org.owasp.html.HtmlPolicyBuilder;
import org.owasp.html.PolicyFactory;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
public class MarkdownDocumentParser {

    private final Parser parser = Parser.builder().build();
    private final HtmlRenderer renderer = HtmlRenderer.builder().build();

    private final PolicyFactory htmlPolicy = new HtmlPolicyBuilder()
            .allowElements(
                    "h1", "h2", "h3", "h4", "h5", "h6",
                    "p", "br",
                    "strong", "em", "del",
                    "ul", "ol", "li",
                    "blockquote",
                    "pre", "code",
                    "a"
            )
            .allowAttributes("href", "title")
            .onElements("a")
            .allowUrlProtocols("http", "https", "mailto")
            .toFactory();

    public Document parse(String slug, String source, String sourcePath) {
        Map<String, String> frontMatter = new HashMap<>();

        String markdown = source;

        if (source.startsWith("---")) {
            int end = source.indexOf("\n---", 3);

            if (end >= 0) {
                String metadata = source.substring(3, end).trim();
                markdown = source.substring(end + 4).trim();

                for (String line : metadata.split("\n")) {
                    int separator = line.indexOf(':');

                    if (separator > 0) {
                        String key = line.substring(0, separator).trim();
                        String value = line.substring(separator + 1).trim();

                        frontMatter.put(key, value);
                    }
                }
            }
        }

        String html = renderHtml(markdown);

        String title = frontMatter.getOrDefault("title", slug);
        String description = frontMatter.getOrDefault("description", "");

        return new Document(
                slug,
                title,
                description,
                markdown,
                html,
                sourcePath
        );
    }

    public String renderHtml(String markdown) {
        Node document = parser.parse(markdown);
        String renderedHtml = renderer.render(document);
        return htmlPolicy.sanitize(renderedHtml);
    }
}
