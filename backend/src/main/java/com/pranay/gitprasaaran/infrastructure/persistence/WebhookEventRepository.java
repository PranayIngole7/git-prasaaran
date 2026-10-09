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
            WHERE (:repositoryId IS NULL OR event.repositoryId = :repositoryId)
              AND (:status IS NULL OR event.status = :status)
              AND (:eventType IS NULL OR LOWER(event.eventType) = LOWER(:eventType))
              AND (:startAt IS NULL OR event.createdAt >= :startAt)
              AND (:endAt IS NULL OR event.createdAt <= :endAt)
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
