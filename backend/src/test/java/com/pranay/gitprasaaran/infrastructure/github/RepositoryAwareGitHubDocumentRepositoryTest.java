package com.pranay.gitprasaaran.infrastructure.github;

import com.pranay.gitprasaaran.domain.document.Document;
import com.pranay.gitprasaaran.domain.repository.Repository;
import com.pranay.gitprasaaran.infrastructure.markdown.MarkdownDocumentParser;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RepositoryAwareGitHubDocumentRepositoryTest {

    private final GitHubClient gitHubClient = mock(GitHubClient.class);
    private final GitHubProperties globalProperties =
            new GitHubProperties("global-owner", "global-repo", "main", "docs", "secret-token");
    private final GitHubDocumentRepository documentRepository = new GitHubDocumentRepository(
            gitHubClient,
            globalProperties,
            new MarkdownDocumentParser(),
            new ObjectMapper()
    );

    @Test
    void shouldUseSuppliedRepositoryConfigurationToFindDocument() {
        Repository repository = repository();
        when(gitHubClient.getFile("team-docs", "handbook", "release", "guides/start.md"))
                .thenReturn(githubFile("# Start"));

        Optional<Document> result = documentRepository.findBySlug(repository, "start");

        assertTrue(result.isPresent());
        assertEquals("guides/start.md", result.orElseThrow().sourcePath());
        verify(gitHubClient).getFile("team-docs", "handbook", "release", "guides/start.md");
    }

    @Test
    void shouldUseSuppliedRepositoryConfigurationToListDocuments() {
        Repository repository = repository();
        when(gitHubClient.listDirectory("team-docs", "handbook", "release", "guides"))
                .thenReturn("""
                        [{"type":"file","name":"start.md"}]
                        """);
        when(gitHubClient.getFile("team-docs", "handbook", "release", "guides/start.md"))
                .thenReturn(githubFile("# Start"));

        List<Document> result = documentRepository.findAll(repository);

        assertEquals(1, result.size());
        assertEquals("start", result.getFirst().slug());
        assertEquals("guides/start.md", result.getFirst().sourcePath());
        verify(gitHubClient).listDirectory("team-docs", "handbook", "release", "guides");
        verify(gitHubClient).getFile("team-docs", "handbook", "release", "guides/start.md");
    }

    @Test
    void shouldKeepGlobalConfigurationPathAvailable() {
        when(gitHubClient.getFile("docs/legacy.md")).thenReturn(githubFile("# Legacy"));

        Optional<Document> result = documentRepository.findBySlug("legacy");

        assertEquals("docs/legacy.md", result.orElseThrow().sourcePath());
        verify(gitHubClient).getFile("docs/legacy.md");
    }

    private static Repository repository() {
        return new Repository(null, "team-docs", "handbook", "release", "guides", true, null, null);
    }

    private static String githubFile(String markdown) {
        String encoded = Base64.getEncoder().encodeToString(markdown.getBytes(StandardCharsets.UTF_8));
        return "{\"content\":\"" + encoded + "\"}";
    }
}
