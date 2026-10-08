package com.pranay.gitprasaaran.domain.repository;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class RepositoryTest {

    @Test
    void shouldAcceptValidConfigurationWithoutGeneratedIdOrTimestamps() {
        assertDoesNotThrow(() -> new Repository(
                null,
                "octo",
                "docs",
                "main",
                "documentation",
                true,
                null,
                null
        ));
    }

    @Test
    void shouldRejectMissingOrBlankConfigurationValues() {
        Stream.of("owner", "name", "branch", "contentPath")
                .forEach(field -> assertThrows(
                        IllegalArgumentException.class,
                        () -> new Repository(
                                null,
                                field.equals("owner") ? " " : "octo",
                                field.equals("name") ? "" : "docs",
                                field.equals("branch") ? null : "main",
                                field.equals("contentPath") ? "\t" : "documentation",
                                true,
                                null,
                                null
                        ),
                        field
                ));
    }

    @Test
    void shouldRejectNonPositiveInternalId() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new Repository(
                        0L,
                        "octo",
                        "docs",
                        "main",
                        "documentation",
                        true,
                        Instant.now(),
                        Instant.now()
                )
        );
    }
}
