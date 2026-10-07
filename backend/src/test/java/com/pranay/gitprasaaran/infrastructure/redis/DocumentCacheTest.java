package com.pranay.gitprasaaran.infrastructure.redis;

import com.pranay.gitprasaaran.domain.document.Document;
import com.pranay.gitprasaaran.infrastructure.github.GitHubProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DocumentCacheTest {

    @Mock
    private RedisTemplate<String, Document> redisTemplate;

    @Mock
    private ValueOperations<String, Document> valueOperations;

    private DocumentCache documentCache;

    @BeforeEach
    void setUp() {
        GitHubProperties properties = new GitHubProperties("octo", "docs", "main", "docs", "token");
        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        documentCache = new DocumentCache(redisTemplate, properties);
    }

    @Test
    void shouldGetDocumentFromCache() {
        Document document = new Document("team", "Team", "desc", "content", "<h1>Team</h1>", "docs/team.md");
        when(valueOperations.get("document:octo:docs:main:team")).thenReturn(document);

        Document result = documentCache.get("team");

        verify(valueOperations).get("document:octo:docs:main:team");
        assertEquals(document, result);
    }

    @Test
    void shouldPutDocumentWithTenMinuteTtl() {
        Document document = new Document("team", "Team", "desc", "content", "<h1>Team</h1>", "docs/team.md");

        documentCache.put("team", document);

        verify(valueOperations).set("document:octo:docs:main:team", document, Duration.ofMinutes(10));
    }

    @Test
    void shouldEvictDocumentFromCache() {
        documentCache.evict("team");

        verify(redisTemplate).delete("document:octo:docs:main:team");
    }

    @Test
    void shouldReturnNullWhenRedisGetFails() {
        when(valueOperations.get("document:octo:docs:main:team")).thenThrow(new RuntimeException("redis down"));

        Document result = documentCache.get("team");

        assertNull(result);
    }

    @Test
    void shouldNotThrowWhenRedisPutFails() {
        Document document = new Document("team", "Team", "desc", "content", "<h1>Team</h1>", "docs/team.md");
        doThrow(new RuntimeException("redis down"))
                .when(valueOperations)
                .set("document:octo:docs:main:team", document, Duration.ofMinutes(10));

        assertDoesNotThrow(() -> documentCache.put("team", document));
    }

    @Test
    void shouldNotThrowWhenRedisEvictionFails() {
        when(redisTemplate.delete("document:octo:docs:main:team")).thenThrow(new RuntimeException("redis down"));

        assertDoesNotThrow(() -> documentCache.evict("team"));
    }
}
