package com.pranay.gitprasaaran.application.activity;

import com.pranay.gitprasaaran.application.repository.RepositoryNotFoundException;
import com.pranay.gitprasaaran.domain.repository.Repository;
import com.pranay.gitprasaaran.domain.repository.RepositoryRepository;
import com.pranay.gitprasaaran.infrastructure.persistence.WebhookEventRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ActivityServiceTest {

    @Mock
    private WebhookEventRepository webhookEventRepository;

    @Mock
    private RepositoryRepository repositoryRepository;

    @InjectMocks
    private ActivityService activityService;

    @Test
    void shouldApplyPaginationFiltersAndStableNewestFirstOrder() {
        Instant from = Instant.parse("2026-01-01T00:00:00Z");
        Instant to = Instant.parse("2026-01-31T23:59:59Z");
        when(webhookEventRepository.findActivity(
                null,
                "FAILED",
                "push",
                from,
                to,
                PageRequest.of(2, 5, Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id")))
        )).thenReturn(new PageImpl<>(List.of(), PageRequest.of(
                2,
                5,
                Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id"))
        ), 0));

        var response = activityService.findActivity(null, 2, 5, "failed", " PuSh ", from, to);

        assertEquals(2, response.page());
        assertEquals(5, response.size());
        assertEquals(0, response.totalElements());
        assertEquals(0, response.totalPages());

        ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
        verify(webhookEventRepository).findActivity(
                org.mockito.ArgumentMatchers.isNull(),
                org.mockito.ArgumentMatchers.eq("FAILED"),
                org.mockito.ArgumentMatchers.eq("push"),
                org.mockito.ArgumentMatchers.eq(from),
                org.mockito.ArgumentMatchers.eq(to),
                pageable.capture()
        );
        assertEquals(List.of(
                new Sort.Order(Sort.Direction.DESC, "createdAt"),
                new Sort.Order(Sort.Direction.DESC, "id")
        ), pageable.getValue().getSort().toList());
    }

    @Test
    void shouldReturnEmptyPageWhenNoEventsMatch() {
        PageRequest request = PageRequest.of(0, 20, Sort.by(
                Sort.Order.desc("createdAt"),
                Sort.Order.desc("id")
        ));
        when(webhookEventRepository.findActivity(null, null, null, null, null, request))
                .thenReturn(new PageImpl<>(List.of(), request, 0));

        var response = activityService.findActivity(null, 0, 20, null, null, null, null);

        assertEquals(List.of(), response.items());
        assertEquals(0, response.totalElements());
    }

    @Test
    void shouldScopeRepositoryActivityToExistingRepository() {
        when(repositoryRepository.findById(17L)).thenReturn(Optional.of(repository()));
        when(webhookEventRepository.findActivity(
                org.mockito.ArgumentMatchers.eq(17L),
                org.mockito.ArgumentMatchers.isNull(),
                org.mockito.ArgumentMatchers.isNull(),
                org.mockito.ArgumentMatchers.isNull(),
                org.mockito.ArgumentMatchers.isNull(),
                org.mockito.ArgumentMatchers.any(Pageable.class)
        )).thenReturn(new PageImpl<>(List.of()));

        activityService.findActivity(17L, 0, 20, null, null, null, null);

        verify(webhookEventRepository).findActivity(
                org.mockito.ArgumentMatchers.eq(17L),
                org.mockito.ArgumentMatchers.isNull(),
                org.mockito.ArgumentMatchers.isNull(),
                org.mockito.ArgumentMatchers.isNull(),
                org.mockito.ArgumentMatchers.isNull(),
                org.mockito.ArgumentMatchers.any(Pageable.class)
        );
    }

    @Test
    void shouldRejectUnknownRepositoryBeforeQueryingEvents() {
        when(repositoryRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(
                RepositoryNotFoundException.class,
                () -> activityService.findActivity(999L, 0, 20, null, null, null, null)
        );
        verify(webhookEventRepository, never()).findActivity(
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(Pageable.class)
        );
    }

    @Test
    void shouldRejectInvalidPageSizeStatusEventTypeAndDateRange() {
        assertThrows(InvalidActivityQueryException.class,
                () -> activityService.findActivity(null, -1, 20, null, null, null, null));
        assertThrows(InvalidActivityQueryException.class,
                () -> activityService.findActivity(null, 0, 0, null, null, null, null));
        assertThrows(InvalidActivityQueryException.class,
                () -> activityService.findActivity(null, 0, 101, null, null, null, null));
        assertThrows(InvalidActivityQueryException.class,
                () -> activityService.findActivity(null, 0, 20, "UNKNOWN", null, null, null));
        assertThrows(InvalidActivityQueryException.class,
                () -> activityService.findActivity(null, 0, 20, null, " ", null, null));
        assertThrows(InvalidActivityQueryException.class,
                () -> activityService.findActivity(
                        null,
                        0,
                        20,
                        null,
                        null,
                        Instant.parse("2026-02-01T00:00:00Z"),
                        Instant.parse("2026-01-01T00:00:00Z")
                ));
        verify(webhookEventRepository, never()).findActivity(
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(Pageable.class)
        );
    }

    private static Repository repository() {
        return new Repository(17L, "octo", "docs", "main", "documentation", true, null, null);
    }
}
