package com.pranay.gitprasaaran.api.document;

import com.pranay.gitprasaaran.application.document.DocumentService;
import com.pranay.gitprasaaran.domain.document.Document;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/documents")
public class DocumentController {

    private final DocumentService documentService;

    public DocumentController(DocumentService documentService) {
        this.documentService = documentService;
    }

    @GetMapping
    public List<Document> findAll() {
        return documentService.findAll();
    }

    @GetMapping("/{slug}")
    public ResponseEntity<Document> findBySlug(@PathVariable String slug) {
        return documentService.findBySlug(slug)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
}
