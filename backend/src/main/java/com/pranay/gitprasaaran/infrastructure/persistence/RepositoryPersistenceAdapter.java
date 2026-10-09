package com.pranay.gitprasaaran.infrastructure.persistence;

import com.pranay.gitprasaaran.domain.repository.Repository;
import com.pranay.gitprasaaran.domain.repository.RepositoryRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
public class RepositoryPersistenceAdapter implements RepositoryRepository {

    private final RepositoryEntityRepository entityRepository;

    public RepositoryPersistenceAdapter(RepositoryEntityRepository entityRepository) {
        this.entityRepository = entityRepository;
    }

    @Override
    public List<Repository> findAll() {
        return entityRepository.findAll().stream()
                .map(RepositoryEntity::toDomain)
                .toList();
    }

    @Override
    public Optional<Repository> findById(Long id) {
        return entityRepository.findById(id)
                .map(RepositoryEntity::toDomain);
    }

    @Override
    public List<Repository> findByOwnerAndNameIgnoreCase(String owner, String name) {
        return entityRepository.findAllByOwnerIgnoreCaseAndNameIgnoreCase(owner, name).stream()
                .map(RepositoryEntity::toDomain)
                .toList();
    }

    @Override
    public boolean existsByOwnerAndName(String owner, String name) {
        return entityRepository.existsByOwnerAndName(owner, name);
    }

    @Override
    public Repository save(Repository repository) {
        return entityRepository.saveAndFlush(new RepositoryEntity(repository)).toDomain();
    }
}
