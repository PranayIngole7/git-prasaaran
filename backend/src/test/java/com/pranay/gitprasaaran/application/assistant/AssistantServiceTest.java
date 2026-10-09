package com.pranay.gitprasaaran.application.assistant;

import com.pranay.gitprasaaran.api.assistant.AssistantRequest;
import com.pranay.gitprasaaran.api.assistant.AssistantResponse;
import com.pranay.gitprasaaran.application.document.DocumentService;
import com.pranay.gitprasaaran.application.repository.RepositoryService;
import com.pranay.gitprasaaran.domain.document.Document;
import com.pranay.gitprasaaran.domain.repository.Repository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AssistantServiceTest {

    @Mock
    private RepositoryService repositoryService;

    @Mock
    private DocumentService documentService;

    @Mock
    private AssistantAnswerGenerator answerGenerator;

    private AssistantService assistantService;

    @BeforeEach
    void setUp() {
        assistantService = new AssistantService(
                repositoryService, documentService, answerGenerator
        );
    }

    @Test
    void shouldAnswerUsingSelectedRepositoryAndReturnSources() {
        Repository repository = repository(true);
        when(repositoryService.findById(1L)).thenReturn(repository);
        when(documentService.findAll(repository)).thenReturn(List.of(
                new Document(
                        "architecture", "Architecture", "",
                        "System architecture details", "",
                        "docs/architecture.md"
                )
        ));
        when(answerGenerator.generate(
                eq("How is the system structured?"),
                contains("System architecture details")
        )).thenReturn("The system uses the documented architecture.");
        when(answerGenerator.modelName()).thenReturn("test-model");

        AssistantResponse response = assistantService.ask(
                new AssistantRequest(1L, "  How is the system structured?  ")
        );

        assertEquals(
                "The system uses the documented architecture.",
                response.answer()
        );
        assertEquals(List.of("docs/architecture.md"), response.sources());
        assertEquals("test-model", response.model());
        verify(answerGenerator).generate(
                eq("How is the system structured?"),
                contains("docs/architecture.md")
        );
    }

    @Test
    void shouldRejectInactiveRepositoryBeforeLoadingDocuments() {
        when(repositoryService.findById(1L)).thenReturn(repository(false));

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> assistantService.ask(new AssistantRequest(1L, "Question"))
        );

        assertEquals(409, exception.getStatusCode().value());
        verifyNoInteractions(documentService, answerGenerator);
    }

    @Test
    void shouldRejectWhenNoUsableDocumentationExists() {
        Repository repository = repository(true);
        when(repositoryService.findById(1L)).thenReturn(repository);
        when(documentService.findAll(repository)).thenReturn(List.of(
                new Document("empty", "Empty", "", "  ", "", "docs/empty.md")
        ));

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> assistantService.ask(new AssistantRequest(1L, "Question"))
        );

        assertEquals(422, exception.getStatusCode().value());
        verifyNoInteractions(answerGenerator);
    }

    @Test
    void shouldBoundEntireContextAndLimitDocumentCount() {
        Repository repository = repository(true);
        when(repositoryService.findById(1L)).thenReturn(repository);

        List<Document> documents = IntStream.range(0, 10)
                .mapToObj(i -> new Document(
                        "doc-" + i, "Document " + i, "",
                        "x".repeat(5000), "", "docs/doc-" + i + ".md"
                ))
                .toList();

        when(documentService.findAll(repository)).thenReturn(documents);
        when(answerGenerator.generate(anyString(), anyString()))
                .thenAnswer(invocation -> {
                    String context = invocation.getArgument(1);
                    assertTrue(
                            context.length() <= 12000,
                            "Context must not exceed 12000 characters"
                    );
                    return "Answer";
                });

        AssistantResponse response = assistantService.ask(
                new AssistantRequest(1L, "Summarize the documentation")
        );

        assertTrue(response.sources().size() <= 6);
        assertTrue(response.sources().size() >= 1);
        verify(answerGenerator).generate(anyString(), anyString());
    }

    @Test
    void shouldIgnoreBlankDocumentsAndFallBackToSlugForSourcePath() {
        Repository repository = repository(true);
        when(repositoryService.findById(1L)).thenReturn(repository);
        when(documentService.findAll(repository)).thenReturn(List.of(
                new Document("blank", "Blank", "", "", "", "docs/blank.md"),
                new Document(
                        "getting-started", "Getting Started", "",
                        "Useful instructions", "", null
                )
        ));
        when(answerGenerator.generate(anyString(), anyString()))
                .thenReturn("Read the getting started guide.");

        AssistantResponse response = assistantService.ask(
                new AssistantRequest(1L, "How do I start?")
        );

        assertEquals(List.of("getting-started"), response.sources());
        verify(answerGenerator).generate(
                eq("How do I start?"),
                contains("Useful instructions")
        );
    }

    private static Repository repository(boolean active) {
        return new Repository(
                1L, "PranayIngole7", "git-prasaaran", "main", "docs",
                active, null, null
        );
    }
}
