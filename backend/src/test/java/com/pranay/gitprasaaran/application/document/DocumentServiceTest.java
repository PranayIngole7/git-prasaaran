package com.pranay.gitprasaaran.application.document;

import com.pranay.gitprasaaran.domain.document.Document;
import com.pranay.gitprasaaran.domain.document.DocumentRepository;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class DocumentServiceTest {

    private final DocumentRepository repository = mock(DocumentRepository.class);
    private final DocumentService service = new DocumentService(repository);

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
    void shouldReturnDocumentBySlug() {
        Document document = new Document(
                "architecture",
                "Architecture",
                "",
                "# Architecture",
                "<h1>Architecture</h1>",
                "docs/architecture.md"
        );

        when(repository.findBySlug("architecture"))
                .thenReturn(Optional.of(document));

        Optional<Document> result = service.findBySlug("architecture");

        assertTrue(result.isPresent());
        assertEquals("architecture", result.get().slug());
        verify(repository).findBySlug("architecture");
    }

    @Test
    void shouldReturnEmptyWhenDocumentDoesNotExist() {
        when(repository.findBySlug("missing"))
                .thenReturn(Optional.empty());

        Optional<Document> result = service.findBySlug("missing");

        assertTrue(result.isEmpty());
        verify(repository).findBySlug("missing");
    }
}
