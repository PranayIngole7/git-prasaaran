package com.pranay.gitprasaaran.infrastructure.persistence;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;

public interface WebhookEventRepository extends JpaRepository<WebhookEventEntity, Long> {

    Optional<WebhookEventEntity> findByEventId(String eventId);

    @Query("""
            SELECT event
            FROM WebhookEventEntity event
            WHERE (event.repositoryId = COALESCE(:repositoryId, event.repositoryId)
                   OR COALESCE(:repositoryId, -1) = -1)
              AND event.status = COALESCE(:status, event.status)
              AND LOWER(event.eventType) = COALESCE(:eventType, LOWER(event.eventType))
              AND event.createdAt >= COALESCE(:startAt, event.createdAt)
              AND event.createdAt <= COALESCE(:endAt, event.createdAt)
            """)
    Page<WebhookEventEntity> findActivity(
            @Param("repositoryId") Long repositoryId,
            @Param("status") String status,
            @Param("eventType") String eventType,
            @Param("startAt") Instant from,
            @Param("endAt") Instant to,
            Pageable pageable
    );
}
