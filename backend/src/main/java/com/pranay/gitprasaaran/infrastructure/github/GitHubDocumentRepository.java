package com.pranay.gitprasaaran.infrastructure.github;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import com.pranay.gitprasaaran.domain.document.Document;
import com.pranay.gitprasaaran.domain.document.DocumentRepository;
import com.pranay.gitprasaaran.infrastructure.markdown.MarkdownDocumentParser;
import org.springframework.stereotype.Repository;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;

@Repository
public class GitHubDocumentRepository implements DocumentRepository {

    private final GitHubClient gitHubClient;
    private final GitHubProperties properties;
    private final MarkdownDocumentParser parser;
    private final ObjectMapper objectMapper;

    public GitHubDocumentRepository(
            GitHubClient gitHubClient,
            GitHubProperties properties,
            MarkdownDocumentParser parser,
            ObjectMapper objectMapper
    ) {
        this.gitHubClient = gitHubClient;
        this.properties = properties;
        this.parser = parser;
        this.objectMapper = objectMapper;
    }

    @Override
public List<Document> findAll() {
    String contentPath = properties.contentPath();

    try {
        String response = gitHubClient.listDirectory(contentPath);
        JsonNode entries = objectMapper.readTree(response);

        if (!entries.isArray()) {
            return List.of();
        }

        return java.util.stream.StreamSupport
                .stream(entries.spliterator(), false)
                .filter(entry -> "file".equals(entry.path("type").asText()))
                .filter(entry -> entry.path("name").asText().endsWith(".md"))
                .map(entry -> entry.path("name").asText())
                .map(this::removeMarkdownExtension)
                .map(this::findBySlug)
                .flatMap(Optional::stream)
                .toList();

    } catch (WebClientResponseException ex) {
        throw new IllegalStateException(
                "Failed to list documents from GitHub",
                ex
        );
    } catch (Exception ex) {
        throw new IllegalStateException(
                "Failed to parse GitHub document listing",
                ex
        );
    }
}

    @Override
    public Optional<Document> findBySlug(String slug) {
        String path = buildPath(slug);

        try {
            String response = gitHubClient.getFile(path);
            JsonNode json = objectMapper.readTree(response);

            if (!json.has("content")) {
                return Optional.empty();
            }

            String encodedContent = json.get("content").asText();
            String source = new String(
                    java.util.Base64.getMimeDecoder().decode(encodedContent),
                    StandardCharsets.UTF_8
            );

            return Optional.of(
                    parser.parse(slug, source, path)
            );

        } catch (WebClientResponseException ex) {
    		if (ex.getStatusCode().value() == 404) {
        	return Optional.empty();
    		}
	
    	  throw new IllegalStateException(
            	"GitHub request failed for document: " + slug,
            	ex
    	  );
	} catch (Exception ex) {
            throw new IllegalStateException(
                    "Failed to load document from GitHub: " + slug,
                    ex
            );
        }
    }

    private String buildPath(String slug) {
        String contentPath = properties.contentPath();

        if (contentPath == null || contentPath.isBlank()) {
            return slug + ".md";
        }

        return contentPath.replaceAll("/$", "") + "/" + slug + ".md";
    }
    
    private String removeMarkdownExtension(String filename) {
    return filename.substring(0, filename.length() - 3);
}
}
