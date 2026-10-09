package com.pranay.gitprasaaran.application.webhook;

import com.pranay.gitprasaaran.domain.repository.Repository;
import com.pranay.gitprasaaran.domain.repository.RepositoryRepository;
import com.pranay.gitprasaaran.infrastructure.persistence.WebhookEventEntity;
import com.pranay.gitprasaaran.infrastructure.persistence.WebhookEventRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.util.Optional;

@Service
public class GitHubWebhookProcessingService {

    private static final Logger log = LoggerFactory.getLogger(GitHubWebhookProcessingService.class);
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final WebhookEventRepository webhookEventRepository;
    private final RepositoryRepository repositoryRepository;
    private final GitHubPushDocumentCacheInvalidator gitHubPushDocumentCacheInvalidator;

    public GitHubWebhookProcessingService(
            WebhookEventRepository webhookEventRepository,
            RepositoryRepository repositoryRepository,
            GitHubPushDocumentCacheInvalidator gitHubPushDocumentCacheInvalidator
    ) {
        this.webhookEventRepository = webhookEventRepository;
        this.repositoryRepository = repositoryRepository;
        this.gitHubPushDocumentCacheInvalidator = gitHubPushDocumentCacheInvalidator;
    }

    @Transactional
    public WebhookProcessingResult process(String deliveryId, String eventType, String payload, String ref, boolean deleted) {
        return process(deliveryId, eventType, payload, ref, deleted, null, null);
    }

    @Transactional
    public WebhookProcessingResult process(
            String deliveryId,
            String eventType,
            String payload,
            String ref,
            boolean deleted,
            String repositoryOwner,
            String repositoryName
    ) {
        if (deliveryId == null || deliveryId.isBlank()) {
            return new WebhookProcessingResult(false, false);
        }

        Optional<WebhookEventEntity> existing = webhookEventRepository.findByEventId(deliveryId);
        if (existing.isPresent()) {
            return new WebhookProcessingResult(true, false);
        }

        String commitSha = extractCommitSha(payload);
        WebhookEventEntity event = new WebhookEventEntity(
                deliveryId,
                normalizeEventType(eventType),
                commitSha,
                "RECEIVED",
                findRepositoryId(repositoryOwner, repositoryName)
        );
        webhookEventRepository.save(event);

        try {
            gitHubPushDocumentCacheInvalidator.process(new GitHubPushEvent(ref, deleted, payload));
            event.markProcessed();
            webhookEventRepository.save(event);
            return new WebhookProcessingResult(false, true);
        } catch (RuntimeException ex) {
            log.warn("Webhook processing failed for delivery '{}' with type '{}'.", deliveryId, eventType, ex);
            event.setStatus("FAILED");
            event.setProcessedAt(null);
            webhookEventRepository.save(event);
            return new WebhookProcessingResult(false, false);
        }
    }

    private String normalizeEventType(String eventType) {
        return eventType == null ? "push" : eventType.trim();
    }

    private String extractCommitSha(String payload) {
        if (payload == null || payload.isBlank()) {
            return null;
        }

        try {
            JsonNode root = OBJECT_MAPPER.readTree(payload);
            if (root == null || !root.isObject()) {
                return null;
            }

            JsonNode headCommit = root.path("head_commit");
            if (headCommit != null && headCommit.isObject()) {
                String sha = headCommit.path("id").asText(null);
                if (sha != null && !sha.isBlank()) {
                    return sha;
                }
            }

            JsonNode commits = root.path("commits");
            if (commits != null && commits.isArray() && !commits.isEmpty()) {
                JsonNode firstCommit = commits.get(0);
                if (firstCommit != null && firstCommit.isObject()) {
                    String sha = firstCommit.path("id").asText(null);
                    if (sha != null && !sha.isBlank()) {
                        return sha;
                    }
                }
            }
        } catch (RuntimeException ignored) {
            // payload is already validated earlier in the controller before this method is called.
        }

        return null;
    }

    private Long findRepositoryId(String owner, String name) {
        if (owner == null || owner.isBlank() || name == null || name.isBlank()) {
            return null;
        }

        var matches = repositoryRepository.findByOwnerAndNameIgnoreCase(owner.trim(), name.trim());
        if (matches.size() != 1) {
            return null;
        }
        Repository repository = matches.getFirst();
        return repository.id();
    }
}
