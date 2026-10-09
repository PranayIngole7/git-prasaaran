package com.pranay.gitprasaaran.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RepositoryEntityRepository extends JpaRepository<RepositoryEntity, Long> {

    List<RepositoryEntity> findAllByOwnerIgnoreCaseAndNameIgnoreCase(String owner, String name);

    boolean existsByOwnerAndName(String owner, String name);
}
