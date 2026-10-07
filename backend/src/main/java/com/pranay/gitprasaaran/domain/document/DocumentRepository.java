package com.pranay.gitprasaaran.domain.document;

import java.util.List;
import java.util.Optional;

public interface DocumentRepository {

    List<Document> findAll();

    Optional<Document> findBySlug(String slug);
}
