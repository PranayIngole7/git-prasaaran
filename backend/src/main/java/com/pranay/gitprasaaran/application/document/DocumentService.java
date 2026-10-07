package com.pranay.gitprasaaran.application.document;

import com.pranay.gitprasaaran.domain.document.Document;
import com.pranay.gitprasaaran.domain.document.DocumentRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class DocumentService {

    private final DocumentRepository documentRepository;

    public DocumentService(DocumentRepository documentRepository) {
        this.documentRepository = documentRepository;
    }

    public List<Document> findAll() {
        return documentRepository.findAll();
    }

    public Optional<Document> findBySlug(String slug) {
        return documentRepository.findBySlug(slug);
    }
}
