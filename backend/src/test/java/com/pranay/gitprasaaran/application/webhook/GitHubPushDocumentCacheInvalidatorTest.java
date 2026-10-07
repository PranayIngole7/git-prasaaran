package com.pranay.gitprasaaran.application.webhook;

import com.pranay.gitprasaaran.application.document.ChangedMarkdownFilesDetector;
import com.pranay.gitprasaaran.infrastructure.github.GitHubProperties;
import com.pranay.gitprasaaran.infrastructure.redis.DocumentCache;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GitHubPushDocumentCacheInvalidatorTest {

    @Mock
    private DocumentCache documentCache;

    @Mock
    private ChangedMarkdownFilesDetector changedMarkdownFilesDetector;

    private GitHubPushDocumentCacheInvalidator invalidator;

    @BeforeEach
    void setUp() {
        invalidator = new GitHubPushDocumentCacheInvalidator(
                documentCache,
                changedMarkdownFilesDetector,
                new GitHubProperties("octo", "docs", "main", "docs", "token")
        );
    }

    @Test
    void shouldEvictAffectedMarkdownDocument() {
        String payload = "{\"commits\":[{\"modified\":[\"docs/guide.md\"]}]}";
        when(changedMarkdownFilesDetector.detect(payload)).thenReturn(List.of("guide"));

        invalidator.process(new GitHubPushEvent("refs/heads/main", false, payload));

        verify(documentCache).evict("guide");
    }

    @Test
    void shouldEvictAllAffectedDocuments() {
        String payload = "{\"commits\":[{\"added\":[\"docs/a.md\"],\"modified\":[\"docs/b.md\",\"docs/c.md\"]}]}";
        when(changedMarkdownFilesDetector.detect(payload)).thenReturn(List.of("a", "b", "c"));

        invalidator.process(new GitHubPushEvent("refs/heads/main", false, payload));

        verify(documentCache).evict("a");
        verify(documentCache).evict("b");
        verify(documentCache).evict("c");
    }

    @Test
    void shouldEvictDuplicateDocumentsOnlyOnce() {
        String payload = "{\"commits\":[{\"added\":[\"docs/guide.md\"],\"modified\":[\"docs/guide.md\"]}]}";
        when(changedMarkdownFilesDetector.detect(payload)).thenReturn(List.of("guide", "guide"));

        invalidator.process(new GitHubPushEvent("refs/heads/main", false, payload));

        verify(documentCache).evict("guide");
    }

    @Test
    void shouldIgnoreNonDocumentFiles() {
        String payload = "{\"commits\":[{\"added\":[\"docs/assets/logo.png\"]}]}";
        when(changedMarkdownFilesDetector.detect(payload)).thenReturn(List.of());

        invalidator.process(new GitHubPushEvent("refs/heads/main", false, payload));

        verify(documentCache, never()).evict("assets/logo");
    }

    @Test
    void shouldIgnoreWrongBranch() {
        String payload = "{\"commits\":[{\"added\":[\"docs/guide.md\"]}]}";

        invalidator.process(new GitHubPushEvent("refs/heads/feature", false, payload));

        verifyNoInteractions(documentCache);
    }

    @Test
    void shouldIgnoreDeletedPush() {
        String payload = "{\"commits\":[{\"removed\":[\"docs/guide.md\"]}]}";

        invalidator.process(new GitHubPushEvent("refs/heads/main", true, payload));

        verifyNoInteractions(documentCache);
    }
}
