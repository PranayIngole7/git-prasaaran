package com.pranay.gitprasaaran.infrastructure.persistence;

import com.pranay.gitprasaaran.api.document.PrivateDocumentRequest;
import com.pranay.gitprasaaran.application.document.DuplicatePrivateDocumentSlugException;
import com.pranay.gitprasaaran.application.document.PrivateDocumentNotFoundException;
import com.pranay.gitprasaaran.application.document.PrivateDocumentService;
import com.pranay.gitprasaaran.infrastructure.markdown.MarkdownDocumentParser;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.TestPropertySource;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DataJpaTest
@TestPropertySource(properties = {
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.flyway.enabled=false"
})
@Import({PrivateDocumentService.class, MarkdownDocumentParser.class})
class PrivateDocumentRepositoryTest {

    @Autowired
    private PrivateDocumentRepository privateDocumentRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PrivateDocumentService privateDocumentService;

    @Test
    void scopesQueriesToTheOwnerAndAllowsTheSameSlugForDifferentOwners() {
        UserEntity alice = userRepository.saveAndFlush(
                new UserEntity("alice@example.com", "hash"));
        UserEntity bob = userRepository.saveAndFlush(
                new UserEntity("bob@example.com", "hash"));
        PrivateDocumentEntity alicesDocument = privateDocumentRepository.saveAndFlush(
                new PrivateDocumentEntity(alice, "Alice note", "notes", "# Alice"));
        PrivateDocumentEntity bobsDocument = privateDocumentRepository.saveAndFlush(
                new PrivateDocumentEntity(bob, "Bob note", "notes", "# Bob"));

        assertThat(privateDocumentRepository
                .findAllByOwnerIdOrderByUpdatedAtDesc(alice.getId()))
                .extracting(PrivateDocumentEntity::getId)
                .containsExactly(alicesDocument.getId());
        assertThat(privateDocumentRepository.findByIdAndOwnerId(
                bobsDocument.getId(), alice.getId())).isEmpty();
        assertThat(privateDocumentRepository.findByIdAndOwnerId(
                bobsDocument.getId(), bob.getId())).isPresent();
    }

    @Test
    void enforcesSlugUniquenessWithinOneOwnerAtTheDatabaseBoundary() {
        UserEntity owner = userRepository.saveAndFlush(
                new UserEntity("owner@example.com", "hash"));
        privateDocumentRepository.saveAndFlush(
                new PrivateDocumentEntity(owner, "First", "shared-slug", "# First"));

        assertThrows(DataIntegrityViolationException.class, () ->
                privateDocumentRepository.saveAndFlush(
                        new PrivateDocumentEntity(
                                owner, "Second", "shared-slug", "# Second")));
    }

    @Test
    void returnsAnEmptyListForAUserWithNoDocuments() {
        UserEntity owner = userRepository.saveAndFlush(
                new UserEntity("empty@example.com", "hash"));

        List<PrivateDocumentEntity> documents =
                privateDocumentRepository.findAllByOwnerIdOrderByUpdatedAtDesc(
                        owner.getId());

        assertThat(documents).isEmpty();
    }

    @Test
    void servicePersistsAndEnforcesOwnerScopeForEveryOperation() {
        UserEntity alice = userRepository.saveAndFlush(
                new UserEntity("alice@example.com", "hash"));
        UserEntity bob = userRepository.saveAndFlush(
                new UserEntity("bob@example.com", "hash"));
        PrivateDocumentRequest request = new PrivateDocumentRequest(
                "Alice notes",
                "notes",
                "# Alice notes\n\n<script>alert('unsafe')</script>");

        var aliceDocument = privateDocumentService.create(
                alice.getEmail(), request);
        privateDocumentService.create(
                bob.getEmail(),
                new PrivateDocumentRequest("Bob notes", "notes", "# Bob notes"));

        assertThat(aliceDocument.id()).isNotNull();
        assertThat(aliceDocument.html()).doesNotContain("<script>");
        assertThat(privateDocumentService.findAll(alice.getEmail()))
                .extracting(document -> document.id())
                .containsExactly(aliceDocument.id());
        assertThat(privateDocumentService.findAll(bob.getEmail()))
                .extracting(document -> document.slug())
                .containsExactly("notes");

        assertThrows(PrivateDocumentNotFoundException.class, () ->
                privateDocumentService.findById(bob.getEmail(), aliceDocument.id()));
        assertThrows(PrivateDocumentNotFoundException.class, () ->
                privateDocumentService.update(
                        bob.getEmail(), aliceDocument.id(), request));
        assertThrows(PrivateDocumentNotFoundException.class, () ->
                privateDocumentService.delete(bob.getEmail(), aliceDocument.id()));

        var updated = privateDocumentService.update(
                alice.getEmail(),
                aliceDocument.id(),
                new PrivateDocumentRequest("Updated notes", "updated-notes", "# Updated"));
        assertThat(updated.title()).isEqualTo("Updated notes");
        assertThat(updated.slug()).isEqualTo("updated-notes");

        assertThrows(DuplicatePrivateDocumentSlugException.class, () ->
                privateDocumentService.create(
                        alice.getEmail(),
                        new PrivateDocumentRequest("Duplicate", "updated-notes", "# Duplicate")));

        privateDocumentService.delete(alice.getEmail(), aliceDocument.id());
        assertThat(privateDocumentService.findAll(alice.getEmail())).isEmpty();
    }
}
