package com.pranay.gitprasaaran.application.activity;

import com.pranay.gitprasaaran.api.activity.ActivityEventResponse;
import com.pranay.gitprasaaran.api.activity.ActivityPageResponse;
import com.pranay.gitprasaaran.application.repository.RepositoryNotFoundException;
import com.pranay.gitprasaaran.domain.repository.RepositoryRepository;
import com.pranay.gitprasaaran.infrastructure.persistence.WebhookEventRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Locale;
import java.util.Set;

@Service
public class ActivityService {

    private static final int MAX_PAGE_SIZE = 100;
    private static final int MAX_EVENT_TYPE_LENGTH = 100;
    private static final Set<String> STATUSES = Set.of("RECEIVED", "PROCESSED", "FAILED");
    private static final Sort ACTIVITY_ORDER = Sort.by(
            Sort.Order.desc("createdAt"),
            Sort.Order.desc("id")
    );

    private final WebhookEventRepository webhookEventRepository;
    private final RepositoryRepository repositoryRepository;

    public ActivityService(
            WebhookEventRepository webhookEventRepository,
            RepositoryRepository repositoryRepository
    ) {
        this.webhookEventRepository = webhookEventRepository;
        this.repositoryRepository = repositoryRepository;
    }

    @Transactional(readOnly = true)
    public ActivityPageResponse findActivity(
            Long repositoryId,
            int page,
            int size,
            String status,
            String eventType,
            Instant from,
            Instant to
    ) {
        if (page < 0) {
            throw new InvalidActivityQueryException("page must be zero or greater");
        }
        if (size < 1 || size > MAX_PAGE_SIZE) {
            throw new InvalidActivityQueryException("size must be between 1 and 100");
        }
        if (from != null && to != null && from.isAfter(to)) {
            throw new InvalidActivityQueryException("from must be less than or equal to to");
        }

        String normalizedStatus = normalizeStatus(status);
        String normalizedEventType = normalizeEventType(eventType);

        if (repositoryId != null && repositoryRepository.findById(repositoryId).isEmpty()) {
            throw new RepositoryNotFoundException(repositoryId);
        }

        Page<ActivityEventResponse> events = webhookEventRepository.findActivity(
                        repositoryId,
                        normalizedStatus,
                        normalizedEventType,
                        from,
                        to,
                        PageRequest.of(page, size, ACTIVITY_ORDER)
                )
                .map(ActivityEventResponse::from);

        return ActivityPageResponse.from(events);
    }

    private String normalizeStatus(String status) {
        if (status == null) {
            return null;
        }

        String normalized = status.trim().toUpperCase(Locale.ROOT);
        if (!STATUSES.contains(normalized)) {
            throw new InvalidActivityQueryException("status must be RECEIVED, PROCESSED, or FAILED");
        }
        return normalized;
    }

    private String normalizeEventType(String eventType) {
        if (eventType == null) {
            return null;
        }

        String normalized = eventType.trim();
        if (normalized.isEmpty() || normalized.length() > MAX_EVENT_TYPE_LENGTH) {
            throw new InvalidActivityQueryException("eventType must contain between 1 and 100 characters");
        }
        return normalized.toLowerCase(Locale.ROOT);
    }
}
