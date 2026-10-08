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
        verifyNoCacheAccess();
    }

    @Test
    void shouldRetrieveDocumentUsingProvidedRepository() {
        Repository repository = repository(true);
        when(documentRepository.findBySlug(repository, "start")).thenReturn(Optional.of(document()));

        assertEquals(Optional.of(document()), documentService.findBySlug(repository, "start"));
        verify(documentRepository).findBySlug(repository, "start");
        verifyNoCacheAccess();
    }

    @Test
    void shouldRejectInactiveRepositoryWithoutRetrieval() {
        Repository repository = repository(false);

        assertThrows(IllegalStateException.class, () -> documentService.findAll(repository));
        assertThrows(IllegalStateException.class, () -> documentService.findBySlug(repository, "start"));
        verify(documentRepository, never()).findAll(repository);
        verify(documentRepository, never()).findBySlug(repository, "start");
        verifyNoCacheAccess();
    }

    @Test
    void shouldRejectMissingRepositoryContext() {
        assertThrows(IllegalArgumentException.class, () -> documentService.findAll(null));
        assertThrows(IllegalArgumentException.class, () -> documentService.findBySlug(null, "start"));
        verifyNoCacheAccess();
    }

    private void verifyNoCacheAccess() {
        org.mockito.Mockito.verifyNoInteractions(documentCache);
    }

    private static Repository repository(boolean active) {
        return new Repository(null, "team-docs", "handbook", "release", "guides", active, null, null);
    }

    private static Document document() {
        return new Document("start", "Start", "", "Content", "<p>Content</p>\n", "guides/start.md");
    }
}
