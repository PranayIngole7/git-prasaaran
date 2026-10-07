package com.pranay.gitprasaaran.domain.document;

public record Document(
        String slug,
        String title,
        String description,
        String content,
        String html,
        String sourcePath
) {
}
