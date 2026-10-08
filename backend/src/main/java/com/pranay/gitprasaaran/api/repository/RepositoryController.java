package com.pranay.gitprasaaran.api.repository;

import com.pranay.gitprasaaran.application.repository.RepositoryService;
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

@RestController
@RequestMapping("/api/v1/repositories")
public class RepositoryController {

    private final RepositoryService repositoryService;

    public RepositoryController(RepositoryService repositoryService) {
        this.repositoryService = repositoryService;
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
