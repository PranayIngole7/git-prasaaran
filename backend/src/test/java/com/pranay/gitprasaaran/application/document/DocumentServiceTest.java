package com.pranay.gitprasaaran.application.document;

import com.pranay.gitprasaaran.domain.document.Document;
import com.pranay.gitprasaaran.domain.document.DocumentRepository;
import com.pranay.gitprasaaran.infrastructure.redis.DocumentCache;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class DocumentServiceTest {

    private final DocumentRepository repository = mock(DocumentRepository.class);
    private final DocumentCache cache = mock(DocumentCache.class);
    private final DocumentService service = new DocumentService(repository, cache);

    @Test
    void shouldReturnAllDocuments() {
        Document document = new Document(
                "architecture",
                "Architecture",
                "",
                "# Architecture",
                "<h1>Architecture</h1>",
                "docs/architecture.md"
        );

        when(repository.findAll()).thenReturn(List.of(document));

        List<Document> result = service.findAll();

        assertEquals(1, result.size());
        assertEquals("architecture", result.getFirst().slug());
        verify(repository).findAll();
    }

    @Test
    void shouldReturnDocumentBySlugFromCacheWithoutRepositoryCall() {
        Document document = new Document(
                "architecture",
                "Architecture",
                "",
                "# Architecture",
                "<h1>Architecture</h1>",
                "docs/architecture.md"
        );

        when(cache.get("architecture")).thenReturn(document);

        Optional<Document> result = service.findBySlug("architecture");

        assertTrue(result.isPresent());
        assertEquals(document, result.get());
        verify(cache).get("architecture");
        verify(cache, never()).put(anyString(), any());
        verifyNoInteractions(repository);
    }

    @Test
    void shouldCacheDocumentFoundByRepository() {
        Document document = new Document(
                "architecture",
                "Architecture",
                "",
                "# Architecture",
                "<h1>Architecture</h1>",
                "docs/architecture.md"
        );

        when(cache.get("architecture")).thenReturn(null);
        when(repository.findBySlug("architecture")).thenReturn(Optional.of(document));

        Optional<Document> result = service.findBySlug("architecture");

        assertTrue(result.isPresent());
        assertEquals(document, result.get());
        verify(repository).findBySlug("architecture");
        verify(cache).put("architecture", document);
    }

    @Test
    void shouldReturnEmptyWhenDocumentDoesNotExistAndNotCacheIt() {
        when(cache.get("missing")).thenReturn(null);
        when(repository.findBySlug("missing")).thenReturn(Optional.empty());

        Optional<Document> result = service.findBySlug("missing");

        assertTrue(result.isEmpty());
        verify(repository).findBySlug("missing");
        verify(cache, never()).put(anyString(), any());
    }
}
