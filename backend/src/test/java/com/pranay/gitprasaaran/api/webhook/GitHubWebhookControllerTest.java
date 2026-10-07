package com.pranay.gitprasaaran.api.webhook;

import com.pranay.gitprasaaran.application.webhook.GitHubWebhookProcessingService;
import com.pranay.gitprasaaran.application.webhook.WebhookProcessingResult;
import com.pranay.gitprasaaran.infrastructure.github.GitHubWebhookSignatureVerifier;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(GitHubWebhookController.class)
class GitHubWebhookControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private GitHubWebhookSignatureVerifier signatureVerifier;

    @MockitoBean
    private GitHubWebhookProcessingService gitHubWebhookProcessingService;

    @Test
    void shouldAcceptValidPushWebhook() throws Exception {
        String payload = "{\"repository\":{\"full_name\":\"octo/docs\"},\"ref\":\"refs/heads/main\",\"after\":\"abc123\",\"before\":\"def456\",\"deleted\":false,\"commits\":[{\"added\":[\"docs/new.md\"],\"modified\":[\"README.md\"],\"removed\":[]}]}";
        when(signatureVerifier.isValid(anyString(), eq("sha256=valid"))).thenReturn(true);
        when(gitHubWebhookProcessingService.process("delivery-123", "push", payload, "refs/heads/main", false))
                .thenReturn(new WebhookProcessingResult(false, true));

        mockMvc.perform(post("/api/v1/webhooks/github")
                        .header("X-Hub-Signature-256", "sha256=valid")
                        .header("X-GitHub-Event", "push")
                        .header("X-GitHub-Delivery", "delivery-123")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.status").value("accepted"))
                .andExpect(jsonPath("$.event").value("push"))
                .andExpect(jsonPath("$.processed").value(true));
    }

    @Test
    void shouldRejectInvalidSignature() throws Exception {
        String payload = "{\"repository\":{\"full_name\":\"octo/docs\"}}";
        when(signatureVerifier.isValid(anyString(), eq("sha256=invalid"))).thenReturn(false);

        mockMvc.perform(post("/api/v1/webhooks/github")
                        .header("X-Hub-Signature-256", "sha256=invalid")
                        .header("X-GitHub-Event", "push")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_WEBHOOK_SIGNATURE"))
                .andExpect(jsonPath("$.message").value("Invalid webhook signature"));

        org.mockito.Mockito.verify(gitHubWebhookProcessingService, org.mockito.Mockito.never())
                .process(org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.anyBoolean());
    }

    @Test
    void shouldIgnoreUnsupportedEvent() throws Exception {
        String payload = "{\"repository\":{\"full_name\":\"octo/docs\"}}";
        when(signatureVerifier.isValid(anyString(), eq("sha256=valid"))).thenReturn(true);

        mockMvc.perform(post("/api/v1/webhooks/github")
                        .header("X-Hub-Signature-256", "sha256=valid")
                        .header("X-GitHub-Event", "ping")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ignored"))
                .andExpect(jsonPath("$.event").value("ping"));
    }

    @Test
    void shouldRejectMalformedJson() throws Exception {
        when(signatureVerifier.isValid(anyString(), eq("sha256=valid"))).thenReturn(true);

        mockMvc.perform(post("/api/v1/webhooks/github")
                        .header("X-Hub-Signature-256", "sha256=valid")
                        .header("X-GitHub-Event", "push")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{not-valid-json"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_WEBHOOK_PAYLOAD"))
                .andExpect(jsonPath("$.message").value("Malformed webhook payload"));
    }

    @Test
    void shouldRejectMissingSignature() throws Exception {
        String payload = "{\"repository\":{\"full_name\":\"octo/docs\"}}";

        mockMvc.perform(post("/api/v1/webhooks/github")
                        .header("X-GitHub-Event", "push")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_WEBHOOK_SIGNATURE"));
    }
}
