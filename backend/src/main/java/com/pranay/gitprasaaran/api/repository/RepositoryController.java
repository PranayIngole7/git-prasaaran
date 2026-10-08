package com.pranay.gitprasaaran.api.repository;

import com.pranay.gitprasaaran.application.document.DocumentService;
import com.pranay.gitprasaaran.application.repository.RepositoryService;
import com.pranay.gitprasaaran.domain.document.Document;
import com.pranay.gitprasaaran.domain.repository.Repository;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/v1/repositories")
public class RepositoryController {

    private final RepositoryService repositoryService;
    private final DocumentService documentService;

    public RepositoryController(RepositoryService repositoryService, DocumentService documentService) {
        this.repositoryService = repositoryService;
        this.documentService = documentService;
    }

    @GetMapping
    public List<RepositoryResponse> findAll() {
        return repositoryService.findAll().stream()
                .map(RepositoryResponse::from)
                .toList();
    }

    @PostMapping
    public ResponseEntity<RepositoryResponse> create(@Valid @RequestBody RepositoryRequest request) {
        RepositoryResponse response = RepositoryResponse.from(repositoryService.create(
                request.owner(),
                request.name(),
                request.branch(),
                request.contentPath()
        ));
        return ResponseEntity.created(ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{repositoryId}")
                .buildAndExpand(response.id())
                .toUri()).body(response);
    }

    @GetMapping("/{repositoryId}")
    public RepositoryResponse findById(@PathVariable Long repositoryId) {
        return RepositoryResponse.from(repositoryService.findById(repositoryId));
    }

    @GetMapping("/{repositoryId}/documents")
    public List<Document> findDocuments(@PathVariable Long repositoryId) {
        Repository repository = repositoryService.findById(repositoryId);
        return documentService.findAll(repository);
    }

    @GetMapping("/{repositoryId}/documents/{slug}")
    public ResponseEntity<Document> findDocumentBySlug(
            @PathVariable Long repositoryId,
            @PathVariable String slug
    ) {
        Repository repository = repositoryService.findById(repositoryId);
        Optional<Document> document = documentService.findBySlug(repository, slug);
        return document.map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PatchMapping("/{repositoryId}")
    public RepositoryResponse update(
            @PathVariable Long repositoryId,
            @Valid @RequestBody RepositoryPatchRequest request
    ) {
        return RepositoryResponse.from(repositoryService.update(
                repositoryId,
                request.owner(),
                request.name(),
                request.branch(),
                request.contentPath(),
                request.active()
        ));
    }
}
