package com.pranay.gitprasaaran.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

public interface RepositoryEntityRepository extends JpaRepository<RepositoryEntity, Long> {

    boolean existsByOwnerAndName(String owner, String name);
}
