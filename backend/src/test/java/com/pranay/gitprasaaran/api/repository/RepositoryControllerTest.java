package com.pranay.gitprasaaran.api.repository;

import com.pranay.gitprasaaran.application.repository.DuplicateRepositoryException;
import com.pranay.gitprasaaran.application.repository.RepositoryNotFoundException;
import com.pranay.gitprasaaran.application.repository.RepositoryService;
import com.pranay.gitprasaaran.application.document.DocumentService;
import com.pranay.gitprasaaran.config.security.JwtAuthenticationFilter;
import com.pranay.gitprasaaran.config.security.JwtService;
import com.pranay.gitprasaaran.config.security.SecurityConfig;
import com.pranay.gitprasaaran.domain.document.Document;
import com.pranay.gitprasaaran.domain.repository.Repository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        controllers = RepositoryController.class,
        properties = "gitprasaaran.security.jwt.secret=test-secret"
)
@Import({
        SecurityConfig.class,
        JwtAuthenticationFilter.class,
        RepositoryControllerTest.SecurityInfrastructure.class
})
class RepositoryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private RepositoryService repositoryService;

    @MockitoBean
    private DocumentService documentService;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private UserDetailsService userDetailsService;

    @Test
    void shouldListRepositoriesForAuthenticatedUser() throws Exception {
        when(repositoryService.findAll()).thenReturn(List.of(repository(1L, true)));

        mockMvc.perform(get("/api/v1/repositories").with(user("customer@example.com").roles("CUSTOMER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].owner").value("octo"));
    }

    @Test
    void shouldRejectUnauthenticatedListRequest() throws Exception {
        mockMvc.perform(get("/api/v1/repositories"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldReturnRepositoryById() throws Exception {
        when(repositoryService.findById(1L)).thenReturn(repository(1L, true));

        mockMvc.perform(get("/api/v1/repositories/1").with(user("customer@example.com").roles("CUSTOMER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("docs"))
                .andExpect(jsonPath("$.active").value(true));
    }

    @Test
    void shouldListDocumentsForSelectedRepository() throws Exception {
        Repository selectedRepository = repository(1L, true);
        Document document = document("readme");
        when(repositoryService.findById(1L)).thenReturn(selectedRepository);
        when(documentService.findAll(selectedRepository)).thenReturn(List.of(document));

        mockMvc.perform(get("/api/v1/repositories/1/documents")
                        .with(user("customer@example.com").roles("CUSTOMER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].slug").value("readme"))
                .andExpect(jsonPath("$[0].sourcePath").value("documentation/readme.md"));

        verify(repositoryService).findById(1L);
        verify(documentService).findAll(selectedRepository);
    }

    @Test
    void shouldReturnDocumentForSelectedRepository() throws Exception {
        Repository selectedRepository = repository(1L, true);
        Document document = document("readme");
        when(repositoryService.findById(1L)).thenReturn(selectedRepository);
        when(documentService.findBySlug(selectedRepository, "readme")).thenReturn(Optional.of(document));

        mockMvc.perform(get("/api/v1/repositories/1/documents/readme")
                        .with(user("customer@example.com").roles("CUSTOMER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.slug").value("readme"))
                .andExpect(jsonPath("$.title").value("README"));

        verify(repositoryService).findById(1L);
        verify(documentService).findBySlug(selectedRepository, "readme");
    }

    @Test
    void shouldReturnNotFoundForDocumentFromSelectedRepositoryWhenMissing() throws Exception {
        Repository selectedRepository = repository(1L, true);
        when(repositoryService.findById(1L)).thenReturn(selectedRepository);
        when(documentService.findBySlug(selectedRepository, "missing")).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/v1/repositories/1/documents/missing")
                        .with(user("customer@example.com").roles("CUSTOMER")))
                .andExpect(status().isNotFound());

        verify(documentService).findBySlug(selectedRepository, "missing");
    }

    @Test
    void shouldReturnNotFoundForDocumentsWhenRepositoryDoesNotExist() throws Exception {
        when(repositoryService.findById(999L)).thenThrow(new RepositoryNotFoundException(999L));

        mockMvc.perform(get("/api/v1/repositories/999/documents")
                        .with(user("customer@example.com").roles("CUSTOMER")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("REPOSITORY_NOT_FOUND"));

        verify(documentService, never()).findAll(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void shouldRejectInactiveRepositoryDocumentListing() throws Exception {
        Repository inactiveRepository = repository(1L, false);
        when(repositoryService.findById(1L)).thenReturn(inactiveRepository);
        when(documentService.findAll(inactiveRepository))
                .thenThrow(new IllegalStateException("Repository is inactive: 1"));

        mockMvc.perform(get("/api/v1/repositories/1/documents")
                        .with(user("customer@example.com").roles("CUSTOMER")))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.code").value("DOCUMENT_RETRIEVAL_FAILED"));

        verify(documentService).findAll(inactiveRepository);
    }

    @Test
    void shouldRejectUnauthenticatedRepositoryDocumentRequest() throws Exception {
        mockMvc.perform(get("/api/v1/repositories/1/documents"))
                .andExpect(status().isUnauthorized());

        verify(repositoryService, never()).findById(1L);
        verify(documentService, never()).findAll(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void shouldReturnNotFoundForUnknownId() throws Exception {
        when(repositoryService.findById(999L)).thenThrow(new RepositoryNotFoundException(999L));

        mockMvc.perform(get("/api/v1/repositories/999").with(user("customer@example.com").roles("CUSTOMER")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("REPOSITORY_NOT_FOUND"));
    }

    @Test
    void shouldForbidNonAdminCreate() throws Exception {
        mockMvc.perform(post("/api/v1/repositories")
                        .with(user("customer@example.com").roles("CUSTOMER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validCreateRequest()))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldCreateRepositoryForAdmin() throws Exception {
        when(repositoryService.create("octo", "docs", "main", "documentation"))
                .thenReturn(repository(7L, true));

        mockMvc.perform(post("/api/v1/repositories")
                        .with(user("admin@example.com").roles("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validCreateRequest()))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "http://localhost/api/v1/repositories/7"))
                .andExpect(jsonPath("$.id").value(7))
                .andExpect(jsonPath("$.owner").value("octo"))
                .andExpect(jsonPath("$.active").value(true));
    }

    @Test
    void shouldRejectInvalidCreateInput() throws Exception {
        mockMvc.perform(post("/api/v1/repositories")
                        .with(user("admin@example.com").roles("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"owner":" ","name":"docs","branch":"main","contentPath":"documentation"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"));
    }

    @Test
    void shouldRejectDuplicateRepositoryRegistration() throws Exception {
        when(repositoryService.create("octo", "docs", "main", "documentation"))
                .thenThrow(new DuplicateRepositoryException("octo", "docs"));

        mockMvc.perform(post("/api/v1/repositories")
                        .with(user("admin@example.com").roles("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validCreateRequest()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("REPOSITORY_ALREADY_EXISTS"));
    }

    @Test
    void shouldUpdateRepositoryForAdmin() throws Exception {
        when(repositoryService.update(1L, null, null, "release", null, null))
                .thenReturn(new Repository(
                        1L,
                        "octo",
                        "docs",
                        "release",
                        "documentation",
                        true,
                        Instant.parse("2026-01-01T00:00:00Z"),
                        Instant.parse("2026-01-02T00:00:00Z")
                ));

        mockMvc.perform(patch("/api/v1/repositories/1")
                        .with(user("admin@example.com").roles("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"branch":"release"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.branch").value("release"));

        verify(repositoryService).update(1L, null, null, "release", null, null);
    }

    @Test
    void shouldForbidNonAdminUpdate() throws Exception {
        mockMvc.perform(patch("/api/v1/repositories/1")
                        .with(user("customer@example.com").roles("CUSTOMER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"active":false}
                                """))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldReturnNotFoundWhenAdminUpdatesUnknownRepository() throws Exception {
        when(repositoryService.update(999L, null, null, null, null, false))
                .thenThrow(new RepositoryNotFoundException(999L));

        mockMvc.perform(patch("/api/v1/repositories/999")
                        .with(user("admin@example.com").roles("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"active":false}
                                """))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("REPOSITORY_NOT_FOUND"));
    }

    @Test
    void shouldDeactivateRepositoryWithActiveFalse() throws Exception {
        when(repositoryService.update(1L, null, null, null, null, false))
                .thenReturn(repository(1L, false));

        mockMvc.perform(patch("/api/v1/repositories/1")
                        .with(user("admin@example.com").roles("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"active":false}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(false));
    }

    @Test
    void shouldRejectEmptyPatch() throws Exception {
        mockMvc.perform(patch("/api/v1/repositories/1")
                        .with(user("admin@example.com").roles("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"));
    }

    private static String validCreateRequest() {
        return """
                {"owner":"octo","name":"docs","branch":"main","contentPath":"documentation"}
                """;
    }

    private static Repository repository(Long id, boolean active) {
        Instant createdAt = Instant.parse("2026-01-01T00:00:00Z");
        return new Repository(id, "octo", "docs", "main", "documentation", active, createdAt, createdAt);
    }

    private static Document document(String slug) {
        return new Document(slug, "README", "", "# README", "<h1>README</h1>", "documentation/" + slug + ".md");
    }

    @TestConfiguration
    @EnableWebSecurity
    static class SecurityInfrastructure {
    }

}
