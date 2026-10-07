package com.pranay.gitprasaaran.api.document;

import com.pranay.gitprasaaran.application.document.DocumentService;
import com.pranay.gitprasaaran.domain.document.Document;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Optional;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(DocumentController.class)
class DocumentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private DocumentService documentService;

    @Test
    void shouldReturnAllDocuments() throws Exception {
        Document document = new Document(
                "architecture",
                "Architecture",
                "",
                "# Architecture",
                "<h1>Architecture</h1>",
                "docs/architecture.md"
        );

        when(documentService.findAll()).thenReturn(List.of(document));

        mockMvc.perform(get("/api/v1/documents"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$[0].slug").value("architecture"))
                .andExpect(jsonPath("$[0].title").value("Architecture"));

        verify(documentService).findAll();
    }

    @Test
    void shouldReturnDocumentBySlug() throws Exception {
        Document document = new Document(
                "architecture",
                "Architecture",
                "",
                "# Architecture",
                "<h1>Architecture</h1>",
                "docs/architecture.md"
        );

        when(documentService.findBySlug("architecture"))
                .thenReturn(Optional.of(document));

        mockMvc.perform(get("/api/v1/documents/architecture"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.slug").value("architecture"))
                .andExpect(jsonPath("$.title").value("Architecture"))
                .andExpect(jsonPath("$.sourcePath").value("docs/architecture.md"));

        verify(documentService).findBySlug("architecture");
    }

    @Test
    void shouldReturnNotFoundWhenDocumentDoesNotExist() throws Exception {
        when(documentService.findBySlug("missing"))
                .thenReturn(Optional.empty());

        mockMvc.perform(get("/api/v1/documents/missing"))
                .andExpect(status().isNotFound())
                .andExpect(content().string(""));

        verify(documentService).findBySlug("missing");
    }

    @Test
    void shouldReturnBadGatewayWhenDocumentRetrievalFails() throws Exception {
        when(documentService.findBySlug("broken"))
                .thenThrow(new IllegalStateException("GitHub unavailable"));

        mockMvc.perform(get("/api/v1/documents/broken"))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.code").value("DOCUMENT_RETRIEVAL_FAILED"))
                .andExpect(jsonPath("$.message")
                        .value("Unable to retrieve document"));
    }
}
