package com.pranay.gitprasaaran.api.assistant;

import com.pranay.gitprasaaran.application.assistant.AssistantService;
import com.pranay.gitprasaaran.config.security.JwtAuthenticationFilter;
import com.pranay.gitprasaaran.config.security.JwtService;
import com.pranay.gitprasaaran.config.security.SecurityConfig;
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

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        controllers = AssistantController.class,
        properties = "gitprasaaran.security.jwt.secret=test-secret"
)
@Import({
        SecurityConfig.class,
        JwtAuthenticationFilter.class,
        AssistantControllerTest.SecurityInfrastructure.class
})
class AssistantControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AssistantService assistantService;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private UserDetailsService userDetailsService;

    @Test
    void shouldRejectUnauthenticatedRequest() throws Exception {
        mockMvc.perform(post("/api/v1/assistant/ask")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"repositoryId":1,"question":"Explain this repository"}
                                """))
                .andExpect(status().isUnauthorized());

        verify(assistantService, never()).ask(any(AssistantRequest.class));
    }

    @Test
    void shouldAllowAuthenticatedCustomerToAskQuestion() throws Exception {
        when(assistantService.ask(any(AssistantRequest.class)))
                .thenReturn(new AssistantResponse(
                        "The project uses Spring Boot.",
                        List.of("README.md"),
                        "test-model"
                ));

        mockMvc.perform(post("/api/v1/assistant/ask")
                        .with(user("customer@example.com").roles("CUSTOMER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"repositoryId":1,"question":"Explain this repository"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.answer").value("The project uses Spring Boot."))
                .andExpect(jsonPath("$.sources[0]").value("README.md"))
                .andExpect(jsonPath("$.model").value("test-model"));

        verify(assistantService).ask(any(AssistantRequest.class));
    }

    @Test
    void shouldRejectInvalidRequestBody() throws Exception {
        mockMvc.perform(post("/api/v1/assistant/ask")
                        .with(user("customer@example.com").roles("CUSTOMER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"repositoryId":0,"question":" "}
                                """))
                .andExpect(status().isBadRequest());

        verify(assistantService, never()).ask(any(AssistantRequest.class));
    }

    @TestConfiguration
    @EnableWebSecurity
    static class SecurityInfrastructure {
    }
}
