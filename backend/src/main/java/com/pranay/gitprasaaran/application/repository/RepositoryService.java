package com.pranay.gitprasaaran.application.repository;

import com.pranay.gitprasaaran.domain.repository.Repository;
import com.pranay.gitprasaaran.domain.repository.RepositoryRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class RepositoryService {

    private final RepositoryRepository repositoryRepository;

    public RepositoryService(RepositoryRepository repositoryRepository) {
        this.repositoryRepository = repositoryRepository;
    }

    @Transactional(readOnly = true)
    public List<Repository> findAll() {
        return repositoryRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Repository findById(Long repositoryId) {
        return repositoryRepository.findById(repositoryId)
                .orElseThrow(() -> new RepositoryNotFoundException(repositoryId));
    }

    @Transactional
    public Repository create(String owner, String name, String branch, String contentPath) {
        if (repositoryRepository.existsByOwnerAndName(owner, name)) {
            throw new DuplicateRepositoryException(owner, name);
        }

        Repository repository = new Repository(
                null,
                owner,
                name,
                branch,
                contentPath,
                true,
                null,
                null
        );

        try {
            return repositoryRepository.save(repository);
        } catch (DataIntegrityViolationException ex) {
            throw new DuplicateRepositoryException(owner, name);
        }
    }

    @Transactional
    public Repository update(
            Long repositoryId,
            String owner,
            String name,
            String branch,
            String contentPath,
            Boolean active
    ) {
        Repository existing = findById(repositoryId);
        String updatedOwner = owner == null ? existing.owner() : owner;
        String updatedName = name == null ? existing.name() : name;

        if ((!updatedOwner.equals(existing.owner()) || !updatedName.equals(existing.name()))
                && repositoryRepository.existsByOwnerAndName(updatedOwner, updatedName)) {
            throw new DuplicateRepositoryException(updatedOwner, updatedName);
        }

        Repository updated = new Repository(
                existing.id(),
                updatedOwner,
                updatedName,
                branch == null ? existing.branch() : branch,
                contentPath == null ? existing.contentPath() : contentPath,
                active == null ? existing.active() : active,
                existing.createdAt(),
                existing.updatedAt()
        );

        try {
            return repositoryRepository.save(updated);
        } catch (DataIntegrityViolationException ex) {
            throw new DuplicateRepositoryException(updatedOwner, updatedName);
        }
    }
}
