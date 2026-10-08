package com.pranay.gitprasaaran.domain.document;

import com.pranay.gitprasaaran.domain.repository.Repository;

import java.util.List;
import java.util.Optional;

public interface DocumentRepository {

    List<Document> findAll();

    Optional<Document> findBySlug(String slug);

    List<Document> findAll(Repository repository);

    Optional<Document> findBySlug(Repository repository, String slug);
}
