package com.pranay.gitprasaaran.api.activity;

import com.pranay.gitprasaaran.application.activity.ActivityService;
import com.pranay.gitprasaaran.application.activity.InvalidActivityQueryException;
import com.pranay.gitprasaaran.config.security.JwtAuthenticationFilter;
import com.pranay.gitprasaaran.config.security.JwtService;
import com.pranay.gitprasaaran.config.security.SecurityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.UserDetailsService;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        controllers = ActivityController.class,
        properties = "gitprasaaran.security.jwt.secret=test-secret"
)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class})
class ActivityControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ActivityService activityService;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private UserDetailsService userDetailsService;

    @Test
    void shouldRequireAuthentication() throws Exception {
        mockMvc.perform(get("/api/v1/activity"))
                .andExpect(status().isUnauthorized());

        verify(activityService, never()).findActivity(
                any(),
                any(Integer.class),
                any(Integer.class),
                any(),
                any(),
                any(),
                any()
        );
    }

    @Test
    void shouldReturnActivityPageDtoForAuthenticatedUser() throws Exception {
        ActivityEventResponse event = new ActivityEventResponse(
                7L,
                "delivery-7",
                "push",
                "abc123",
                "PROCESSED",
                Instant.parse("2026-01-01T00:00:00Z"),
                Instant.parse("2026-01-01T00:00:01Z"),
                17L
        );
        when(activityService.findActivity(null, 0, 20, null, null, null, null))
                .thenReturn(new ActivityPageResponse(List.of(event), 0, 20, 1, 1));

        mockMvc.perform(get("/api/v1/activity")
                        .with(user("customer@example.com").roles("CUSTOMER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].id").value(7))
                .andExpect(jsonPath("$.items[0].deliveryId").value("delivery-7"))
                .andExpect(jsonPath("$.items[0].repositoryId").value(17))
                .andExpect(jsonPath("$.items[0].eventType").value("push"))
                .andExpect(jsonPath("$.items[0].commitSha").value("abc123"))
                .andExpect(jsonPath("$.items[0].status").value("PROCESSED"))
                .andExpect(jsonPath("$.items[0].createdAt").value("2026-01-01T00:00:00Z"))
                .andExpect(jsonPath("$.items[0].processedAt").value("2026-01-01T00:00:01Z"))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(20))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.totalPages").value(1));
    }

    @Test
    void shouldPassRepositoryScopeAndQueryFilters() throws Exception {
        when(activityService.findActivity(
                eq(17L),
                eq(1),
                eq(10),
                eq("FAILED"),
                eq("push"),
                eq(Instant.parse("2026-01-01T00:00:00Z")),
                eq(Instant.parse("2026-01-31T00:00:00Z"))
        )).thenReturn(new ActivityPageResponse(List.of(), 1, 10, 0, 0));

        mockMvc.perform(get("/api/v1/repositories/17/activity")
                        .param("page", "1")
                        .param("size", "10")
                        .param("status", "FAILED")
                        .param("eventType", "push")
                        .param("from", "2026-01-01T00:00:00Z")
                        .param("to", "2026-01-31T00:00:00Z")
                        .with(user("customer@example.com").roles("CUSTOMER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items").isEmpty());
    }

    @Test
    void shouldReturnBadRequestForInvalidQueryAndTimestamp() throws Exception {
        when(activityService.findActivity(null, -1, 20, null, null, null, null))
                .thenThrow(new InvalidActivityQueryException("page must be zero or greater"));

        mockMvc.perform(get("/api/v1/activity")
                        .param("page", "-1")
                        .with(user("customer@example.com").roles("CUSTOMER")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_ACTIVITY_QUERY"));

        mockMvc.perform(get("/api/v1/activity")
                        .param("from", "not-a-timestamp")
                        .with(user("customer@example.com").roles("CUSTOMER")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"));
    }

    @Test
    void shouldReturnNotFoundForUnknownRepository() throws Exception {
        when(activityService.findActivity(eq(999L), eq(0), eq(20), any(), any(), any(), any()))
                .thenThrow(new com.pranay.gitprasaaran.application.repository.RepositoryNotFoundException(999L));

        mockMvc.perform(get("/api/v1/repositories/999/activity")
                        .with(user("customer@example.com").roles("CUSTOMER")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("REPOSITORY_NOT_FOUND"));
    }

    @TestConfiguration
    @EnableWebSecurity
    static class SecurityInfrastructure {
    }
}
