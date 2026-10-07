package com.pranay.gitprasaaran.application.document;

import com.pranay.gitprasaaran.infrastructure.github.GitHubProperties;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Component
public class ChangedMarkdownFilesDetector {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final GitHubProperties githubProperties;

    public ChangedMarkdownFilesDetector(GitHubProperties githubProperties) {
        this.githubProperties = githubProperties;
    }

    public List<String> detect(String payload) {
        if (payload == null || payload.isBlank()) {
            return List.of();
        }

        try {
            JsonNode root = OBJECT_MAPPER.readTree(payload);
            if (root == null || !root.isObject()) {
                return List.of();
            }

            Set<String> slugs = new LinkedHashSet<>();
            JsonNode commits = root.path("commits");
            if (commits != null && commits.isArray()) {
                for (JsonNode commit : commits) {
                    if (commit == null || !commit.isObject()) {
                        continue;
                    }

                    collectPaths(slugs, commit.path("added"));
                    collectPaths(slugs, commit.path("modified"));
                    collectPaths(slugs, commit.path("removed"));
                }
            }

            return new ArrayList<>(slugs);
        } catch (Exception ex) {
            return List.of();
        }
    }

    private void collectPaths(Set<String> slugs, JsonNode paths) {
        if (paths == null || !paths.isArray()) {
            return;
        }

        for (JsonNode pathNode : paths) {
            if (pathNode == null || pathNode.isNull()) {
                continue;
            }

            String slug = toSlug(pathNode.asText(null));
            if (slug != null && !slug.isBlank()) {
                slugs.add(slug);
            }
        }
    }

    private String toSlug(String path) {
        String normalized = normalize(path);
        if (normalized == null) {
            return null;
        }

        String contentPath = normalize(githubProperties.contentPath());
        if (contentPath == null || contentPath.isBlank()) {
            return null;
        }

        if (!normalized.equals(contentPath) && !normalized.startsWith(contentPath + "/")) {
            return null;
        }

        if (normalized.equals(contentPath) || normalized.endsWith("/")) {
            return null;
        }

        String relativePath = normalized.equals(contentPath)
                ? ""
                : normalized.substring(contentPath.length() + 1);

        if (!relativePath.endsWith(".md") || relativePath.isBlank()) {
            return null;
        }

        String slug = relativePath.substring(0, relativePath.length() - 3);
        return slug.isBlank() ? null : slug;
    }

    private String normalize(String path) {
        if (path == null) {
            return null;
        }

        String normalized = path.trim().replace('\\', '/');
        if (normalized.isBlank()) {
            return null;
        }

        normalized = normalized.replaceAll("^/+|/+$", "");
        if (normalized.isBlank()) {
            return null;
        }

        return normalized;
    }
}
