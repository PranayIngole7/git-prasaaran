package com.pranay.gitprasaaran.application.document;

import com.pranay.gitprasaaran.api.document.PrivateDocumentRequest;
import com.pranay.gitprasaaran.api.document.PrivateDocumentResponse;
import com.pranay.gitprasaaran.infrastructure.markdown.MarkdownDocumentParser;
import com.pranay.gitprasaaran.infrastructure.persistence.PrivateDocumentEntity;
import com.pranay.gitprasaaran.infrastructure.persistence.PrivateDocumentRepository;
import com.pranay.gitprasaaran.infrastructure.persistence.UserEntity;
import com.pranay.gitprasaaran.infrastructure.persistence.UserRepository;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;

@Service
@Transactional(readOnly = true)
public class PrivateDocumentService {

    private static final String OWNER_SLUG_CONSTRAINT =
            "uk_private_documents_owner_slug";

    private final UserRepository userRepository;
    private final PrivateDocumentRepository privateDocumentRepository;
    private final MarkdownDocumentParser markdownDocumentParser;

    public PrivateDocumentService(
            UserRepository userRepository,
            PrivateDocumentRepository privateDocumentRepository,
            MarkdownDocumentParser markdownDocumentParser) {
        this.userRepository = userRepository;
        this.privateDocumentRepository = privateDocumentRepository;
        this.markdownDocumentParser = markdownDocumentParser;
    }

    public List<PrivateDocumentResponse> findAll(String email) {
        Long ownerId = requireOwner(email).getId();
        return privateDocumentRepository
                .findAllByOwnerIdOrderByUpdatedAtDesc(ownerId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public PrivateDocumentResponse findById(String email, Long id) {
        Long ownerId = requireOwner(email).getId();
        return privateDocumentRepository.findByIdAndOwnerId(id, ownerId)
                .map(this::toResponse)
                .orElseThrow(PrivateDocumentNotFoundException::new);
    }

    @Transactional
    public PrivateDocumentResponse create(
            String email,
            PrivateDocumentRequest request) {
        UserEntity owner = requireOwner(email);
        String slug = normalizeSlug(request.slug());
        if (privateDocumentRepository.existsByOwnerIdAndSlug(owner.getId(), slug)) {
            throw new DuplicatePrivateDocumentSlugException();
        }

        PrivateDocumentEntity document = new PrivateDocumentEntity(
                owner,
                request.title().trim(),
                slug,
                request.content());

        return saveAndRespond(document);
    }

    @Transactional
    public PrivateDocumentResponse update(
            String email,
            Long id,
            PrivateDocumentRequest request) {
        Long ownerId = requireOwner(email).getId();
        PrivateDocumentEntity document = privateDocumentRepository
                .findByIdAndOwnerId(id, ownerId)
                .orElseThrow(PrivateDocumentNotFoundException::new);
        String slug = normalizeSlug(request.slug());

        if (!document.getSlug().equals(slug)
                && privateDocumentRepository.existsByOwnerIdAndSlug(ownerId, slug)) {
            throw new DuplicatePrivateDocumentSlugException();
        }

        document.update(request.title().trim(), slug, request.content());
        return saveAndRespond(document);
    }

    @Transactional
    public void delete(String email, Long id) {
        Long ownerId = requireOwner(email).getId();
        PrivateDocumentEntity document = privateDocumentRepository
                .findByIdAndOwnerId(id, ownerId)
                .orElseThrow(PrivateDocumentNotFoundException::new);
        privateDocumentRepository.delete(document);
    }

    private UserEntity requireOwner(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(PrivateDocumentNotFoundException::new);
    }

    private PrivateDocumentResponse saveAndRespond(
            PrivateDocumentEntity document) {
        try {
            PrivateDocumentEntity saved =
                    privateDocumentRepository.saveAndFlush(document);
            return toResponse(saved);
        } catch (DataIntegrityViolationException exception) {
            if (isOwnerSlugConflict(exception)) {
                throw new DuplicatePrivateDocumentSlugException();
            }
            throw exception;
        }
    }

    private boolean isOwnerSlugConflict(Throwable exception) {
        for (Throwable cause = exception; cause != null; cause = cause.getCause()) {
            if (cause instanceof ConstraintViolationException violation
                    && OWNER_SLUG_CONSTRAINT.equals(violation.getConstraintName())) {
                return true;
            }
        }
        return false;
    }

    private String normalizeSlug(String slug) {
        return slug.trim().toLowerCase(Locale.ROOT);
    }

    private PrivateDocumentResponse toResponse(
            PrivateDocumentEntity document) {
        String html = markdownDocumentParser.renderHtml(document.getContent());
        return PrivateDocumentResponse.from(document, html);
    }
}
