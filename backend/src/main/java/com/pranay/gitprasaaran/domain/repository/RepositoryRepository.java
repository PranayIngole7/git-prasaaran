package com.pranay.gitprasaaran.domain.repository;

import java.util.List;
import java.util.Optional;

public interface RepositoryRepository {

    List<Repository> findAll();

    Optional<Repository> findById(Long id);

    List<Repository> findByOwnerAndNameIgnoreCase(String owner, String name);

    boolean existsByOwnerAndName(String owner, String name);

    Repository save(Repository repository);
}
