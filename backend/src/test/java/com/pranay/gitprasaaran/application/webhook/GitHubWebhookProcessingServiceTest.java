package com.pranay.gitprasaaran.application.webhook;

import com.pranay.gitprasaaran.infrastructure.persistence.WebhookEventEntity;
import com.pranay.gitprasaaran.infrastructure.persistence.WebhookEventRepository;
import com.pranay.gitprasaaran.domain.repository.Repository;
import com.pranay.gitprasaaran.domain.repository.RepositoryRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GitHubWebhookProcessingServiceTest {

    @Mock
    private WebhookEventRepository webhookEventRepository;

    @Mock
    private RepositoryRepository repositoryRepository;

    @Mock
    private GitHubPushDocumentCacheInvalidator gitHubPushDocumentCacheInvalidator;

    @InjectMocks
    private GitHubWebhookProcessingService processingService;

    @Test
    void shouldPersistNewDeliveryAndMarkProcessed() {
        String payload = "{\"head_commit\":{\"id\":\"abc123\"},\"commits\":[{\"id\":\"abc123\"}]}";
        when(webhookEventRepository.findByEventId("delivery-123")).thenReturn(Optional.empty());

        WebhookProcessingResult result = processingService.process("delivery-123", "push", payload, "refs/heads/main", false);

        ArgumentCaptor<WebhookEventEntity> captor = ArgumentCaptor.forClass(WebhookEventEntity.class);
        verify(webhookEventRepository, atLeastOnce()).save(captor.capture());
        WebhookEventEntity savedEvent = captor.getAllValues().getLast();

        assertFalse(result.duplicate());
        assertTrue(result.processed());
        assertEquals("delivery-123", savedEvent.getEventId());
        assertEquals("push", savedEvent.getEventType());
        assertEquals("PROCESSED", savedEvent.getStatus());
        assertNotNull(savedEvent.getProcessedAt());
    }

    @Test
    void shouldIgnoreDuplicateDelivery() {
        WebhookEventEntity existing = new WebhookEventEntity("delivery-123", "push", "abc123", "PROCESSED");
        when(webhookEventRepository.findByEventId("delivery-123")).thenReturn(Optional.of(existing));

        WebhookProcessingResult result = processingService.process("delivery-123", "push", "{\"head_commit\":{\"id\":\"abc123\"}}", "refs/heads/main", false);

        assertTrue(result.duplicate());
        assertFalse(result.processed());
        verify(webhookEventRepository, never()).save(org.mockito.ArgumentMatchers.any());
        verify(gitHubPushDocumentCacheInvalidator, never()).process(org.mockito.ArgumentMatchers.any());
        verify(repositoryRepository, never()).findByOwnerAndNameIgnoreCase(
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.anyString()
        );
    }

    @Test
    void shouldStoreCommitSha() {
        String payload = "{\"head_commit\":{\"id\":\"commit-789\"}}";
        when(webhookEventRepository.findByEventId("delivery-456")).thenReturn(Optional.empty());

        processingService.process("delivery-456", "push", payload, "refs/heads/main", false);

        ArgumentCaptor<WebhookEventEntity> captor = ArgumentCaptor.forClass(WebhookEventEntity.class);
        verify(webhookEventRepository, atLeastOnce()).save(captor.capture());

        assertEquals("commit-789", captor.getAllValues().getLast().getCommitSha());
    }

    @Test
    void shouldAssociateDeliveryWithConfiguredRepository() {
        String payload = "{\"head_commit\":{\"id\":\"abc123\"}}";
        when(webhookEventRepository.findByEventId("delivery-known")).thenReturn(Optional.empty());
        when(repositoryRepository.findByOwnerAndNameIgnoreCase("octo", "docs"))
                .thenReturn(List.of(repository(17L)));

        processingService.process(
                "delivery-known",
                "push",
                payload,
                "refs/heads/main",
                false,
                "octo",
                "docs"
        );

        ArgumentCaptor<WebhookEventEntity> captor = ArgumentCaptor.forClass(WebhookEventEntity.class);
        verify(webhookEventRepository, atLeastOnce()).save(captor.capture());
        assertEquals(17L, captor.getAllValues().getLast().getRepositoryId());
    }

    @Test
    void shouldLeaveUnknownRepositoryUnassociated() {
        when(webhookEventRepository.findByEventId("delivery-unknown")).thenReturn(Optional.empty());
        when(repositoryRepository.findByOwnerAndNameIgnoreCase("other", "docs")).thenReturn(List.of());

        processingService.process(
                "delivery-unknown",
                "push",
                "{}",
                "refs/heads/main",
                false,
                "other",
                "docs"
        );

        ArgumentCaptor<WebhookEventEntity> captor = ArgumentCaptor.forClass(WebhookEventEntity.class);
        verify(webhookEventRepository, atLeastOnce()).save(captor.capture());
        assertNull(captor.getAllValues().getLast().getRepositoryId());
        assertEquals("PROCESSED", captor.getAllValues().getLast().getStatus());
    }

    @Test
    void shouldNotAssociateAmbiguousCaseInsensitiveRepositoryMatch() {
        when(webhookEventRepository.findByEventId("delivery-ambiguous")).thenReturn(Optional.empty());
        when(repositoryRepository.findByOwnerAndNameIgnoreCase("octo", "docs"))
                .thenReturn(List.of(repository(17L), repository(18L)));

        processingService.process(
                "delivery-ambiguous",
                "push",
                "{}",
                "refs/heads/main",
                false,
                "octo",
                "docs"
        );

        ArgumentCaptor<WebhookEventEntity> captor = ArgumentCaptor.forClass(WebhookEventEntity.class);
        verify(webhookEventRepository, atLeastOnce()).save(captor.capture());
        assertNull(captor.getAllValues().getLast().getRepositoryId());
    }

    @Test
    void shouldKeepProcessingWhenRedisInvalidationFails() {
        String payload = "{\"head_commit\":{\"id\":\"abc123\"}}";
        when(webhookEventRepository.findByEventId("delivery-789")).thenReturn(Optional.empty());
        org.mockito.Mockito.doThrow(new RuntimeException("redis down"))
                .when(gitHubPushDocumentCacheInvalidator)
                .process(org.mockito.ArgumentMatchers.any());

        WebhookProcessingResult result = processingService.process("delivery-789", "push", payload, "refs/heads/main", false);

        assertFalse(result.duplicate());
        assertFalse(result.processed());
        ArgumentCaptor<WebhookEventEntity> captor = ArgumentCaptor.forClass(WebhookEventEntity.class);
        verify(webhookEventRepository, atLeastOnce()).save(captor.capture());
        assertEquals("FAILED", captor.getAllValues().getLast().getStatus());
    }

    private static Repository repository(Long id) {
        return new Repository(
                id,
                "octo",
                "docs",
                "main",
                "documentation",
                true,
                null,
                null
        );
    }
}
