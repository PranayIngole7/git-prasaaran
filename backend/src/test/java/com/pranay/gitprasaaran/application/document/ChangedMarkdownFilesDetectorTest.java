package com.pranay.gitprasaaran.application.document;

import com.pranay.gitprasaaran.infrastructure.github.GitHubProperties;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ChangedMarkdownFilesDetectorTest {

    private final ChangedMarkdownFilesDetector detector =
            new ChangedMarkdownFilesDetector(new GitHubProperties("octo", "docs", "main", "docs", "token"));

    @Test
    void shouldDetectAddedMarkdownFile() {
        String payload = "{\"commits\":[{\"added\":[\"docs/architecture.md\"]}]}";

        assertEquals(List.of("architecture"), detector.detect(payload));
    }

    @Test
    void shouldDetectModifiedMarkdownFile() {
        String payload = "{\"commits\":[{\"modified\":[\"docs/security.md\"]}]}";

        assertEquals(List.of("security"), detector.detect(payload));
    }

    @Test
    void shouldDetectRemovedMarkdownFile() {
        String payload = "{\"commits\":[{\"removed\":[\"docs/summary.md\"]}]}";

        assertEquals(List.of("summary"), detector.detect(payload));
    }

    @Test
    void shouldIgnoreDuplicatePathsAcrossCommits() {
        String payload = "{\"commits\":[{\"added\":[\"docs/architecture.md\"]},{\"modified\":[\"docs/architecture.md\",\"docs/security.md\"]},{\"removed\":[\"docs/security.md\"]}]}";

        assertEquals(List.of("architecture", "security"), detector.detect(payload));
    }

    @Test
    void shouldIgnoreNonMarkdownFiles() {
        String payload = "{\"commits\":[{\"added\":[\"docs/assets/logo.png\"]}]}";

        assertEquals(List.of(), detector.detect(payload));
    }

    @Test
    void shouldIgnoreFilesOutsideContentPath() {
        String payload = "{\"commits\":[{\"added\":[\"README.md\"]}]}";

        assertEquals(List.of(), detector.detect(payload));
    }

    @Test
    void shouldHandleNestedMarkdownPath() {
        String payload = "{\"commits\":[{\"added\":[\"docs/guides/getting-started.md\"]}]}";

        assertEquals(List.of("guides/getting-started"), detector.detect(payload));
    }

    @Test
    void shouldIgnoreNullAndEmptyPaths() {
        String payload = "{\"commits\":[{\"added\":[\"\",\"null\",\"docs/guide.md\",\"docs/\"]}]}";

        assertEquals(List.of("guide"), detector.detect(payload));
    }

    @Test
    void shouldCombineMultipleCommitsCorrectly() {
        String payload = "{\"commits\":[{\"added\":[\"docs/a.md\"]},{\"modified\":[\"docs/b.md\",\"docs/c.md\"]},{\"removed\":[\"docs/a.md\",\"docs/d.md\"]}]}";

        assertEquals(List.of("a", "b", "c", "d"), detector.detect(payload));
    }
}
