package com.pranay.gitprasaaran.api.document;

import com.pranay.gitprasaaran.api.error.GlobalExceptionHandler;
import com.pranay.gitprasaaran.application.document.DuplicatePrivateDocumentSlugException;
import com.pranay.gitprasaaran.application.document.PrivateDocumentNotFoundException;
import com.pranay.gitprasaaran.application.document.PrivateDocumentService;
import com.pranay.gitprasaaran.config.security.SecurityConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.http.HttpStatus;
import org.springframework.context.annotation.Import;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        controllers = PrivateDocumentController.class,
        excludeFilters = {
                @ComponentScan.Filter(
                        type = FilterType.ASSIGNABLE_TYPE,
                        classes = SecurityConfig.class),
                @ComponentScan.Filter(
                        type = FilterType.ASSIGNABLE_TYPE,
                        classes = com.pranay.gitprasaaran.config.security.JwtAuthenticationFilter.class)
        })
@Import({
        GlobalExceptionHandler.class,
        PrivateDocumentControllerTest.TestSecurityConfig.class
})
class PrivateDocumentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PrivateDocumentService privateDocumentService;

    private PrivateDocumentResponse response;

    @BeforeEach
    void setUp() {
        response = new PrivateDocumentResponse(
                5L,
                "Private note",
                "private-note",
                "# Private",
                "<h1>Private</h1>\n",
                Instant.parse("2026-01-01T00:00:00Z"),
                Instant.parse("2026-01-01T00:00:00Z"));
    }

    @Test
    void rejectsUnauthenticatedRequestsWithoutCallingService() throws Exception {
        mockMvc.perform(get("/api/v1/private-documents"))
                .andExpect(status().isUnauthorized());

        verify(privateDocumentService, never()).findAll(any());
    }

    @Test
    void createsForTheAuthenticatedPrincipalAndIgnoresClientOwnerId()
            throws Exception {
        when(privateDocumentService.create(
                eq("owner@example.com"), any(PrivateDocumentRequest.class)))
                .thenReturn(response);

        mockMvc.perform(post("/api/v1/private-documents")
                        .with(user("owner@example.com").roles("CUSTOMER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "ownerId": 999,
                                  "title": "Private note",
                                  "slug": "private-note",
                                  "content": "# Private"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "http://localhost/api/v1/private-documents/5"))
                .andExpect(jsonPath("$.id").value(5))
                .andExpect(jsonPath("$.html").value("<h1>Private</h1>\n"))
                .andExpect(jsonPath("$.ownerId").doesNotExist());

        verify(privateDocumentService).create(
                eq("owner@example.com"), any(PrivateDocumentRequest.class));
    }

    @Test
    void listsOnlyDocumentsForTheAuthenticatedPrincipal() throws Exception {
        when(privateDocumentService.findAll("owner@example.com"))
                .thenReturn(List.of(response));

        mockMvc.perform(get("/api/v1/private-documents")
                        .with(user("owner@example.com").roles("CUSTOMER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(5))
                .andExpect(jsonPath("$[0].title").value("Private note"));

        verify(privateDocumentService).findAll("owner@example.com");
    }

    @Test
    void returnsNotFoundForDocumentsOutsideTheAuthenticatedOwnersScope()
            throws Exception {
        when(privateDocumentService.findById("owner@example.com", 55L))
                .thenThrow(new PrivateDocumentNotFoundException());
        when(privateDocumentService.update(
                eq("owner@example.com"), eq(55L), any(PrivateDocumentRequest.class)))
                .thenThrow(new PrivateDocumentNotFoundException());
        org.mockito.Mockito.doThrow(new PrivateDocumentNotFoundException())
                .when(privateDocumentService).delete("owner@example.com", 55L);

        mockMvc.perform(get("/api/v1/private-documents/55")
                        .with(user("owner@example.com").roles("CUSTOMER")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("PRIVATE_DOCUMENT_NOT_FOUND"));

        mockMvc.perform(put("/api/v1/private-documents/55")
                        .with(user("owner@example.com").roles("CUSTOMER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validRequest()))
                .andExpect(status().isNotFound());

        mockMvc.perform(delete("/api/v1/private-documents/55")
                        .with(user("owner@example.com").roles("CUSTOMER")))
                .andExpect(status().isNotFound());

        verify(privateDocumentService).findById("owner@example.com", 55L);
        verify(privateDocumentService).update(
                eq("owner@example.com"), eq(55L), any(PrivateDocumentRequest.class));
        verify(privateDocumentService).delete("owner@example.com", 55L);
    }

    @Test
    void returnsValidationErrorForInvalidDocumentFields() throws Exception {
        mockMvc.perform(post("/api/v1/private-documents")
                        .with(user("owner@example.com").roles("CUSTOMER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": " ",
                                  "slug": "../not-safe",
                                  "content": " "
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"));

        verify(privateDocumentService, never())
                .create(any(), any(PrivateDocumentRequest.class));
    }

    @Test
    void returnsConflictForDuplicateOwnerScopedSlug() throws Exception {
        when(privateDocumentService.create(
                eq("owner@example.com"), any(PrivateDocumentRequest.class)))
                .thenThrow(new DuplicatePrivateDocumentSlugException());

        mockMvc.perform(post("/api/v1/private-documents")
                        .with(user("owner@example.com").roles("CUSTOMER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validRequest()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code")
                        .value("PRIVATE_DOCUMENT_SLUG_ALREADY_EXISTS"));
    }

    private String validRequest() {
        return """
                {
                  "title": "Private note",
                  "slug": "private-note",
                  "content": "# Private"
                }
                """;
    }

    @TestConfiguration
    @EnableWebSecurity
    static class TestSecurityConfig {

        @Bean
        SecurityFilterChain privateDocumentTestSecurityFilterChain(
                HttpSecurity http) throws Exception {
            http
                    .csrf(csrf -> csrf.ignoringRequestMatchers(
                            "/api/v1/private-documents/**"))
                    .authorizeHttpRequests(authorize -> authorize
                            .requestMatchers("/api/v1/private-documents/**")
                            .authenticated()
                            .anyRequest().permitAll())
                    .exceptionHandling(exceptionHandling -> exceptionHandling
                            .authenticationEntryPoint(
                                    new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)));

            return http.build();
        }
    }
}
