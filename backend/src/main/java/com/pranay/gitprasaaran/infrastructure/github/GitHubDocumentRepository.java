package com.pranay.gitprasaaran.infrastructure.github;

import com.pranay.gitprasaaran.domain.document.Document;
import com.pranay.gitprasaaran.domain.document.DocumentRepository;
import com.pranay.gitprasaaran.domain.repository.Repository;
import com.pranay.gitprasaaran.infrastructure.markdown.MarkdownDocumentParser;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;

@org.springframework.stereotype.Repository
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
            return parseDirectory(response, this::findBySlug);
        } catch (WebClientResponseException ex) {
            throw new IllegalStateException("Failed to list documents from GitHub", ex);
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to parse GitHub document listing", ex);
        }
    }

    @Override
    public List<Document> findAll(Repository repository) {
        try {
            String response = gitHubClient.listDirectory(
                    repository.owner(),
                    repository.name(),
                    repository.branch(),
                    repository.contentPath()
            );
            return parseDirectory(response, slug -> findBySlug(repository, slug));
        } catch (WebClientResponseException ex) {
            throw new IllegalStateException("Failed to list documents from GitHub", ex);
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to parse GitHub document listing", ex);
        }
    }

    @Override
    public Optional<Document> findBySlug(String slug) {
        String path = buildPath(properties.contentPath(), slug);

        try {
            String response = gitHubClient.getFile(path);
            return parseDocument(slug, path, response);
        } catch (WebClientResponseException ex) {
            if (ex.getStatusCode().value() == 404) {
                return Optional.empty();
            }
            throw new IllegalStateException("GitHub request failed for document: " + slug, ex);
        } catch (Exception ex) {
            throw new IllegalStateException(
                    "Failed to load document from GitHub: " + slug,
                    ex
            );
        }
    }

    @Override
    public Optional<Document> findBySlug(Repository repository, String slug) {
        String path = buildPath(repository.contentPath(), slug);

        try {
            String response = gitHubClient.getFile(
                    repository.owner(),
                    repository.name(),
                    repository.branch(),
                    path
            );
            return parseDocument(slug, path, response);
        } catch (WebClientResponseException ex) {
            if (ex.getStatusCode().value() == 404) {
                return Optional.empty();
            }
            throw new IllegalStateException("GitHub request failed for document: " + slug, ex);
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to load document from GitHub: " + slug, ex);
        }
    }

    private List<Document> parseDirectory(
            String response,
            java.util.function.Function<String, Optional<Document>> documentFinder
    )
            throws Exception {
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
                .map(documentFinder)
                .flatMap(Optional::stream)
                .toList();
    }

    private Optional<Document> parseDocument(String slug, String path, String response) throws Exception {
        JsonNode json = objectMapper.readTree(response);
        if (!json.has("content")) {
            return Optional.empty();
        }

        String encodedContent = json.get("content").asText();
        String source = new String(
                java.util.Base64.getMimeDecoder().decode(encodedContent),
                StandardCharsets.UTF_8
        );

        return Optional.of(parser.parse(slug, source, path));
    }

    private String buildPath(String contentPath, String slug) {
        if (contentPath == null || contentPath.isBlank()) {
            return slug + ".md";
        }

        return contentPath.replaceAll("/$", "") + "/" + slug + ".md";
    }

    private String removeMarkdownExtension(String filename) {
        return filename.substring(0, filename.length() - 3);
    }
}
