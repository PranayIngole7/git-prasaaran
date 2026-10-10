package com.pranay.gitprasaaran.api.document;

import com.pranay.gitprasaaran.application.document.PrivateDocumentService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.util.List;

@RestController
@RequestMapping("/api/v1/private-documents")
public class PrivateDocumentController {

    private final PrivateDocumentService privateDocumentService;

    public PrivateDocumentController(
            PrivateDocumentService privateDocumentService) {
        this.privateDocumentService = privateDocumentService;
    }

    @GetMapping
    public List<PrivateDocumentResponse> findAll(Authentication authentication) {
        return privateDocumentService.findAll(authentication.getName());
    }

    @PostMapping
    public ResponseEntity<PrivateDocumentResponse> create(
            Authentication authentication,
            @Valid @RequestBody PrivateDocumentRequest request) {
        PrivateDocumentResponse response =
                privateDocumentService.create(authentication.getName(), request);
        return ResponseEntity.created(ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(response.id())
                .toUri())
                .body(response);
    }

    @GetMapping("/{id}")
    public PrivateDocumentResponse findById(
            Authentication authentication,
            @PathVariable Long id) {
        return privateDocumentService.findById(authentication.getName(), id);
    }

    @PutMapping("/{id}")
    public PrivateDocumentResponse update(
            Authentication authentication,
            @PathVariable Long id,
            @Valid @RequestBody PrivateDocumentRequest request) {
        return privateDocumentService.update(
                authentication.getName(), id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            Authentication authentication,
            @PathVariable Long id) {
        privateDocumentService.delete(authentication.getName(), id);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }
}
