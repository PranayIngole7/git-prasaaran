package com.pranay.gitprasaaran.infrastructure.redis;

import com.pranay.gitprasaaran.domain.document.Document;
import com.pranay.gitprasaaran.infrastructure.github.GitHubProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
public class DocumentCache {

    private static final Logger log = LoggerFactory.getLogger(DocumentCache.class);
    private static final Duration TTL = Duration.ofMinutes(10);

    private final RedisTemplate<String, Document> redisTemplate;
    private final GitHubProperties githubProperties;

    public DocumentCache(RedisTemplate<String, Document> redisTemplate, GitHubProperties githubProperties) {
        this.redisTemplate = redisTemplate;
        this.githubProperties = githubProperties;
    }

    public Document get(String slug) {
        try {
            return redisTemplate.opsForValue().get(buildCacheKey(slug));
        } catch (RuntimeException ex) {
            log.warn("Redis cache read failed for slug '{}' using key '{}'. Falling back to GitHub.", slug, buildCacheKey(slug), ex);
            return null;
        }
    }

    public Document get(Long repositoryId, String slug) {
        String key = buildCacheKey(repositoryId, slug);
        try {
            return redisTemplate.opsForValue().get(key);
        } catch (RuntimeException ex) {
            log.warn("Redis cache read failed for repository '{}' and slug '{}' using key '{}'. Falling back to GitHub.",
                    repositoryId, slug, key, ex);
            return null;
        }
    }

    public void put(String slug, Document document) {
        String key = buildCacheKey(slug);
        try {
            redisTemplate.opsForValue().set(key, document, TTL);
        } catch (RuntimeException ex) {
            log.warn("Redis cache write failed for slug '{}' using key '{}'.", slug, key, ex);
        }
    }

    public void put(Long repositoryId, String slug, Document document) {
        String key = buildCacheKey(repositoryId, slug);
        try {
            redisTemplate.opsForValue().set(key, document, TTL);
        } catch (RuntimeException ex) {
            log.warn("Redis cache write failed for repository '{}' and slug '{}' using key '{}'.",
                    repositoryId, slug, key, ex);
        }
    }

    public void evict(String slug) {
        String key = buildCacheKey(slug);
        try {
            redisTemplate.delete(key);
        } catch (RuntimeException ex) {
            log.warn("Redis cache eviction failed for slug '{}' using key '{}'.", slug, key, ex);
        }
    }

    private String buildCacheKey(String slug) {
        return "document:"
                + githubProperties.owner() + ":"
                + githubProperties.repository() + ":"
                + githubProperties.branch() + ":"
                + slug;
    }

    private String buildCacheKey(Long repositoryId, String slug) {
        if (repositoryId == null || repositoryId <= 0) {
            throw new IllegalArgumentException("A persisted repository ID is required for repository document caching");
        }
        if (slug == null || slug.isBlank()) {
            throw new IllegalArgumentException("Document slug must not be blank");
        }
        return "document:" + repositoryId + ":" + slug;
    }
}
