package com.pranay.gitprasaaran.infrastructure.redis;

import com.pranay.gitprasaaran.domain.document.Document;
import com.pranay.gitprasaaran.infrastructure.github.GitHubProperties;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class RedisCacheIntegrationTest {

    private LettuceConnectionFactory connectionFactory;
    private RedisTemplate<String, Document> redisTemplate;
    private DocumentCache documentCache;
    private String slug;

    @BeforeEach
    void setUp() {
        String host = System.getenv().getOrDefault("REDIS_HOST", "localhost");
        int port = Integer.parseInt(System.getenv().getOrDefault("REDIS_PORT", "6379"));

        connectionFactory = new LettuceConnectionFactory(host, port);
        connectionFactory.afterPropertiesSet();

        redisTemplate = new RedisConfig().documentRedisTemplate(connectionFactory);
        documentCache = new DocumentCache(
                redisTemplate,
                new GitHubProperties(
                        "integration-test-owner",
                        "integration-test-repository",
                        "integration-test-branch",
                        "docs",
                        ""
                )
        );

        slug = "redis-integration-" + UUID.randomUUID();
    }

    @AfterEach
    void tearDown() {
        if (redisTemplate != null && slug != null) {
            redisTemplate.delete("document:integration-test-owner:integration-test-repository:"
                    + "integration-test-branch:" + slug);
            redisTemplate.delete("document:987654321:" + slug);
        }

        if (connectionFactory != null) {
            connectionFactory.destroy();
        }
    }

    @Test
    void storesAndRetrievesSerializedDocumentsFromRedis() {
        Document globalDocument = new Document(
                slug, "Integration Test", "Redis integration test",
                "# Hello Redis", "<h1>Hello Redis</h1>", "docs/" + slug + ".md"
        );
        Document repositoryDocument = new Document(
                slug, "Repository Document", "Repository-scoped cache entry",
                "# Repository", "<h1>Repository</h1>", "docs/" + slug + ".md"
        );

        documentCache.put(slug, globalDocument);
        documentCache.put(987654321L, slug, repositoryDocument);

        assertThat(documentCache.get(slug)).isEqualTo(globalDocument);
        assertThat(documentCache.get(987654321L, slug)).isEqualTo(repositoryDocument);

        assertThat(redisTemplate.getExpire(
                "document:integration-test-owner:integration-test-repository:"
                        + "integration-test-branch:" + slug
        )).isPositive();
    }

    @Test
    void evictsGlobalDocumentWithoutAffectingRepositoryScopedEntry() {
        Document globalDocument = new Document(
                slug, "Global", "", "global content", "<p>global</p>", "docs/" + slug + ".md"
        );
        Document repositoryDocument = new Document(
                slug, "Scoped", "", "scoped content", "<p>scoped</p>", "docs/" + slug + ".md"
        );

        documentCache.put(slug, globalDocument);
        documentCache.put(987654321L, slug, repositoryDocument);

        documentCache.evict(slug);

        assertThat(documentCache.get(slug)).isNull();
        assertThat(documentCache.get(987654321L, slug)).isEqualTo(repositoryDocument);
    }
}
