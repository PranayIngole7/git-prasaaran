package com.pranay.gitprasaaran.application.webhook;

import com.pranay.gitprasaaran.application.document.ChangedMarkdownFilesDetector;
import com.pranay.gitprasaaran.infrastructure.github.GitHubProperties;
import com.pranay.gitprasaaran.infrastructure.redis.DocumentCache;
import org.springframework.stereotype.Component;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Component
public class GitHubPushDocumentCacheInvalidator {

    private static final String REFS_HEADS_PREFIX = "refs/heads/";

    private final DocumentCache documentCache;
    private final ChangedMarkdownFilesDetector changedMarkdownFilesDetector;
    private final GitHubProperties gitHubProperties;

    public GitHubPushDocumentCacheInvalidator(
            DocumentCache documentCache,
            ChangedMarkdownFilesDetector changedMarkdownFilesDetector,
            GitHubProperties gitHubProperties
    ) {
        this.documentCache = documentCache;
        this.changedMarkdownFilesDetector = changedMarkdownFilesDetector;
        this.gitHubProperties = gitHubProperties;
    }

    public void process(GitHubPushEvent gitHubPushEvent) {
        if (gitHubPushEvent == null || gitHubPushEvent.payload() == null || gitHubPushEvent.payload().isBlank()) {
            return;
        }

        if (gitHubPushEvent.deleted()) {
            return;
        }

        String ref = gitHubPushEvent.ref();
        if (ref == null || ref.isBlank()) {
            return;
        }

        String expectedRef = REFS_HEADS_PREFIX + normalizeBranch(gitHubProperties.branch());
        if (!expectedRef.equals(ref.trim())) {
            return;
        }

        List<String> affectedSlugs = changedMarkdownFilesDetector.detect(gitHubPushEvent.payload());
        if (affectedSlugs == null || affectedSlugs.isEmpty()) {
            return;
        }

        Set<String> uniqueSlugs = new LinkedHashSet<>(affectedSlugs);
        for (String slug : uniqueSlugs) {
            if (slug != null && !slug.isBlank()) {
                documentCache.evict(slug);
            }
        }
    }

    private String normalizeBranch(String branch) {
        if (branch == null) {
            return "";
        }

        String normalized = branch.trim();
        if (normalized.startsWith(REFS_HEADS_PREFIX)) {
            return normalized.substring(REFS_HEADS_PREFIX.length());
        }

        return normalized;
    }
}
