package com.pranay.gitprasaaran.domain.repository;

import java.util.List;
import java.util.Optional;

public interface RepositoryRepository {

    List<Repository> findAll();

    Optional<Repository> findById(Long id);

    boolean existsByOwnerAndName(String owner, String name);

    Repository save(Repository repository);
}
