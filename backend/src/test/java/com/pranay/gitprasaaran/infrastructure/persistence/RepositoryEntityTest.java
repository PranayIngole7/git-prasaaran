package com.pranay.gitprasaaran.infrastructure.persistence;

import com.pranay.gitprasaaran.domain.repository.Repository;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RepositoryEntityTest {

    @Test
    void shouldMapPersistenceFieldsToDomain() {
        Instant createdAt = Instant.parse("2026-01-01T00:00:00Z");
        Instant updatedAt = Instant.parse("2026-01-02T00:00:00Z");
        RepositoryEntity entity = new RepositoryEntity(new Repository(
                17L,
                "octo",
                "docs",
                "main",
                "documentation",
                false,
                createdAt,
                updatedAt
        ));

        assertEquals(
                new Repository(17L, "octo", "docs", "main", "documentation", false, createdAt, updatedAt),
                entity.toDomain()
        );
    }

    @Test
    void shouldInitializeTimestampsOnPersist() {
        RepositoryEntity entity = new RepositoryEntity(new Repository(
                null,
                "octo",
                "docs",
                "main",
                "documentation",
                true,
                null,
                null
        ));

        entity.prePersist();

        assertNotNull(entity.getCreatedAt());
        assertEquals(entity.getCreatedAt(), entity.getUpdatedAt());
    }

    @Test
    void shouldRefreshUpdatedAtWithoutChangingCreatedAt() {
        Instant createdAt = Instant.parse("2026-01-01T00:00:00Z");
        Instant previousUpdatedAt = Instant.parse("2026-01-02T00:00:00Z");
        RepositoryEntity entity = new RepositoryEntity(new Repository(
                17L,
                "octo",
                "docs",
                "main",
                "documentation",
                true,
                createdAt,
                previousUpdatedAt
        ));

        entity.preUpdate();

        assertEquals(createdAt, entity.getCreatedAt());
        assertTrue(entity.getUpdatedAt().isAfter(previousUpdatedAt));
    }
}
