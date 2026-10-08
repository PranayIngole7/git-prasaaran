package com.pranay.gitprasaaran.domain.repository;

import java.time.Instant;

public record Repository(
        Long id,
        String owner,
        String name,
        String branch,
        String contentPath,
        boolean active,
        Instant createdAt,
        Instant updatedAt
) {

    public Repository {
        requireNonBlank(owner, "owner");
        requireNonBlank(name, "name");
        requireNonBlank(branch, "branch");
        requireNonBlank(contentPath, "contentPath");

        if (id != null && id <= 0) {
            throw new IllegalArgumentException("id must be positive when provided");
        }
    }

    private static void requireNonBlank(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
    }
}
