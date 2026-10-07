package com.pranay.gitprasaaran.application.document;

import com.pranay.gitprasaaran.domain.document.Document;
import com.pranay.gitprasaaran.domain.document.DocumentRepository;
import com.pranay.gitprasaaran.infrastructure.redis.DocumentCache;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class DocumentService {

    private final DocumentRepository documentRepository;
    private final DocumentCache documentCache;

    public DocumentService(DocumentRepository documentRepository, DocumentCache documentCache) {
        this.documentRepository = documentRepository;
        this.documentCache = documentCache;
    }

    public List<Document> findAll() {
        return documentRepository.findAll();
    }

    public Optional<Document> findBySlug(String slug) {
        Document cachedDocument = documentCache.get(slug);
        if (cachedDocument != null) {
            return Optional.of(cachedDocument);
        }

        Optional<Document> document = documentRepository.findBySlug(slug);
        document.ifPresent(value -> documentCache.put(slug, value));
        return document;
    }
}
