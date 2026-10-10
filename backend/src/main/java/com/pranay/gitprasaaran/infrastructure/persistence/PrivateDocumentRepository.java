package com.pranay.gitprasaaran.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PrivateDocumentRepository
        extends JpaRepository<PrivateDocumentEntity, Long> {

    List<PrivateDocumentEntity> findAllByOwnerIdOrderByUpdatedAtDesc(Long ownerId);

    Optional<PrivateDocumentEntity> findByIdAndOwnerId(Long id, Long ownerId);

    boolean existsByOwnerIdAndSlug(Long ownerId, String slug);
}
