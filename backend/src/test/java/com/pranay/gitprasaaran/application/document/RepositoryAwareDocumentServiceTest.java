package com.pranay.gitprasaaran.application.document;

import com.pranay.gitprasaaran.domain.document.Document;
import com.pranay.gitprasaaran.domain.document.DocumentRepository;
import com.pranay.gitprasaaran.domain.repository.Repository;
import com.pranay.gitprasaaran.infrastructure.redis.DocumentCache;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class RepositoryAwareDocumentServiceTest {

    private final DocumentRepository documentRepository = mock(DocumentRepository.class);
    private final DocumentCache documentCache = mock(DocumentCache.class);
    private final DocumentService documentService = new DocumentService(documentRepository, documentCache);

    @Test
    void shouldRetrieveDocumentsUsingProvidedRepository() {
        Repository repository = repository(true);
        List<Document> documents = List.of(document());
        when(documentRepository.findAll(repository)).thenReturn(documents);

        assertEquals(documents, documentService.findAll(repository));
        verify(documentRepository).findAll(repository);
        verifyNoInteractions(documentCache);
    }

    @Test
    void shouldRetrieveDocumentUsingProvidedRepository() {
        Repository repository = repository(true);
        when(documentRepository.findBySlug(repository, "start")).thenReturn(Optional.of(document()));
        when(documentCache.get(repository.id(), "start")).thenReturn(null);

        assertEquals(Optional.of(document()), documentService.findBySlug(repository, "start"));
        verify(documentCache).get(repository.id(), "start");
        verify(documentCache).put(repository.id(), "start", document());
        verify(documentRepository).findBySlug(repository, "start");
    }

    @Test
    void shouldReturnRepositoryScopedCacheHitWithoutRetrieval() {
        Repository repository = repository(true);
        Document cachedDocument = document();
        when(documentCache.get(repository.id(), "start")).thenReturn(cachedDocument);

        assertEquals(Optional.of(cachedDocument), documentService.findBySlug(repository, "start"));
        verify(documentCache).get(repository.id(), "start");
        verify(documentRepository, never()).findBySlug(repository, "start");
    }

    @Test
    void shouldNotUseLegacyKeyForRepositoryAwareMiss() {
        Repository repository = repository(true);
        when(documentCache.get(repository.id(), "start")).thenReturn(null);
        when(documentRepository.findBySlug(repository, "start")).thenReturn(Optional.empty());

        assertEquals(Optional.empty(), documentService.findBySlug(repository, "start"));

        verify(documentCache).get(repository.id(), "start");
        verify(documentCache, never()).get("start");
        verify(documentRepository).findBySlug(repository, "start");
    }

    @Test
    void shouldRejectInactiveRepositoryWithoutRetrieval() {
        Repository repository = repository(false);

        assertThrows(IllegalStateException.class, () -> documentService.findAll(repository));
        assertThrows(IllegalStateException.class, () -> documentService.findBySlug(repository, "start"));
        verify(documentRepository, never()).findAll(repository);
        verify(documentRepository, never()).findBySlug(repository, "start");
        verifyNoInteractions(documentCache);
    }

    @Test
    void shouldRejectMissingRepositoryContext() {
        assertThrows(IllegalArgumentException.class, () -> documentService.findAll(null));
        assertThrows(IllegalArgumentException.class, () -> documentService.findBySlug(null, "start"));
        verifyNoInteractions(documentCache);
    }

    private static Repository repository(boolean active) {
        return new Repository(73L, "team-docs", "handbook", "release", "guides", active, null, null);
    }

    private static Document document() {
        return new Document("start", "Start", "", "Content", "<p>Content</p>\n", "guides/start.md");
    }
}
