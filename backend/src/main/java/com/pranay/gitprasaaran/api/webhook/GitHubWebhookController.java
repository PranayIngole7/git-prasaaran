package com.pranay.gitprasaaran.api.webhook;

import com.pranay.gitprasaaran.api.error.ApiError;
import com.pranay.gitprasaaran.application.webhook.GitHubWebhookProcessingService;
import com.pranay.gitprasaaran.application.webhook.WebhookProcessingResult;
import com.pranay.gitprasaaran.infrastructure.github.GitHubWebhookSignatureVerifier;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/webhooks")
public class GitHubWebhookController {

    private final GitHubWebhookSignatureVerifier signatureVerifier;
    private final GitHubWebhookProcessingService gitHubWebhookProcessingService;
    private final ObjectMapper objectMapper;

    public GitHubWebhookController(
            GitHubWebhookSignatureVerifier signatureVerifier,
            GitHubWebhookProcessingService gitHubWebhookProcessingService,
            ObjectMapper objectMapper
    ) {
        this.signatureVerifier = signatureVerifier;
        this.gitHubWebhookProcessingService = gitHubWebhookProcessingService;
        this.objectMapper = objectMapper;
    }

    @PostMapping("/github")
    public ResponseEntity<?> handleGitHubWebhook(
            @RequestHeader(value = "X-Hub-Signature-256", required = false) String signature,
            @RequestHeader(value = "X-GitHub-Event", required = false, defaultValue = "") String gitHubEvent,
            @RequestHeader(value = "X-GitHub-Delivery", required = false, defaultValue = "") String delivery,
            @RequestBody String payload
    ) {
        if (signature == null || signature.isBlank() || !signatureVerifier.isValid(payload, signature)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ApiError("INVALID_WEBHOOK_SIGNATURE", "Invalid webhook signature"));
        }

        String eventName = gitHubEvent == null ? "" : gitHubEvent.trim();
        if (eventName.isEmpty() || !"push".equalsIgnoreCase(eventName)) {
            return ResponseEntity.ok(Map.of("status", "ignored", "event", eventName));
        }

        try {
            JsonNode payloadNode = objectMapper.readTree(payload);
            payloadNode.path("repository").path("full_name").asText();
            String ref = payloadNode.path("ref").asText(null);
            String after = payloadNode.path("after").asText();
            String before = payloadNode.path("before").asText();
            boolean deleted = payloadNode.path("deleted").asBoolean(false);

            JsonNode commits = payloadNode.path("commits");
            for (JsonNode commit : commits) {
                commit.path("added");
                commit.path("modified");
                commit.path("removed");
            }

            WebhookProcessingResult processingResult = gitHubWebhookProcessingService.process(
                    delivery,
                    eventName,
                    payload,
                    ref,
                    deleted
            );

            return ResponseEntity.accepted().body(Map.of(
                    "status", "accepted",
                    "event", eventName,
                    "duplicate", processingResult.duplicate(),
                    "processed", processingResult.processed()
            ));
        } catch (Exception ex) {
            return ResponseEntity.badRequest()
                    .body(new ApiError("INVALID_WEBHOOK_PAYLOAD", "Malformed webhook payload"));
        }
    }
}
