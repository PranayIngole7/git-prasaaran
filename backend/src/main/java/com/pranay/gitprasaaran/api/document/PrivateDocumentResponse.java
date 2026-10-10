package com.pranay.gitprasaaran.api.document;

import com.pranay.gitprasaaran.infrastructure.persistence.PrivateDocumentEntity;

import java.time.Instant;

public record PrivateDocumentResponse(
        Long id,
        String title,
        String slug,
        String content,
        String html,
        Instant createdAt,
        Instant updatedAt) {

    public static PrivateDocumentResponse from(
            PrivateDocumentEntity document,
            String html) {
        return new PrivateDocumentResponse(
                document.getId(),
                document.getTitle(),
                document.getSlug(),
                document.getContent(),
                html,
                document.getCreatedAt(),
                document.getUpdatedAt());
    }
}
