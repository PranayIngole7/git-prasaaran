package com.pranay.gitprasaaran.application.webhook;

import com.pranay.gitprasaaran.infrastructure.persistence.WebhookEventEntity;
import com.pranay.gitprasaaran.infrastructure.persistence.WebhookEventRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
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
}
