package com.pranay.gitprasaaran.api.activity;

import com.pranay.gitprasaaran.infrastructure.persistence.WebhookEventEntity;

import java.time.Instant;

public record ActivityEventResponse(
        Long id,
        String deliveryId,
        String eventType,
        String commitSha,
        String status,
        Instant createdAt,
        Instant processedAt,
        Long repositoryId
) {

    public static ActivityEventResponse from(WebhookEventEntity event) {
        return new ActivityEventResponse(
                event.getId(),
                event.getEventId(),
                event.getEventType(),
                event.getCommitSha(),
                event.getStatus(),
                event.getCreatedAt(),
                event.getProcessedAt(),
                event.getRepositoryId()
        );
    }
}
