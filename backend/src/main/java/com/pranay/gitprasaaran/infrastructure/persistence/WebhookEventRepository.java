package com.pranay.gitprasaaran.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface WebhookEventRepository extends JpaRepository<WebhookEventEntity, Long> {

    Optional<WebhookEventEntity> findByEventId(String eventId);
}
