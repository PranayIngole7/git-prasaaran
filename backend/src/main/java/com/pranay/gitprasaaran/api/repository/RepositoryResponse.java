package com.pranay.gitprasaaran.api.repository;

import com.pranay.gitprasaaran.domain.repository.Repository;

import java.time.Instant;

public record RepositoryResponse(
        Long id,
        String owner,
        String name,
        String branch,
        String contentPath,
        boolean active,
        Instant createdAt,
        Instant updatedAt
) {

    public static RepositoryResponse from(Repository repository) {
        return new RepositoryResponse(
                repository.id(),
                repository.owner(),
                repository.name(),
                repository.branch(),
                repository.contentPath(),
                repository.active(),
                repository.createdAt(),
                repository.updatedAt()
        );
    }
}
