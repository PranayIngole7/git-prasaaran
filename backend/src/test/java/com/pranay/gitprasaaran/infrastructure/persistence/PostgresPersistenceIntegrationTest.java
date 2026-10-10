package com.pranay.gitprasaaran.infrastructure.persistence;

import com.pranay.gitprasaaran.domain.repository.Repository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.TestPropertySource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:postgresql://localhost:5432/gitprasaaran_integration_test",
        "spring.datasource.username=gitprasaaran",
        "spring.datasource.password=gitprasaaran",
        "spring.jpa.hibernate.ddl-auto=validate",
        "spring.flyway.enabled=true"
})
class PostgresPersistenceIntegrationTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private RepositoryEntityRepository repositoryEntityRepository;

    @Autowired
    private WebhookEventRepository webhookEventRepository;

    @Test
    void flywayCreatesSchemaAndSeedsExpectedRoles() {
        assertThat(roleRepository.findByName(Role.CUSTOMER)).isPresent();
        assertThat(roleRepository.findByName(Role.SUPPORT)).isPresent();
        assertThat(roleRepository.findByName(Role.ADMIN)).isPresent();
    }

    @Test
    void persistsAndReadsUserAndRepository() {
        UserEntity user = userRepository.saveAndFlush(
                new UserEntity("postgres-test@example.com", "test-hash"));

        assertThat(user.getId()).isNotNull();
        assertThat(userRepository.findByEmail("postgres-test@example.com"))
                .isPresent()
                .get()
                .extracting(UserEntity::getEmail)
                .isEqualTo("postgres-test@example.com");

        RepositoryEntity repository = repositoryEntityRepository.saveAndFlush(
                new RepositoryEntity(new Repository(
                        null,
                        "integration-owner",
                        "integration-docs",
                        "main",
                        "docs",
                        true,
                        null,
                        null
                )));

        assertThat(repository.getId()).isNotNull();
        assertThat(repository.getCreatedAt()).isNotNull();
        assertThat(repository.getUpdatedAt()).isNotNull();
        assertThat(repositoryEntityRepository
                .existsByOwnerAndName("integration-owner", "integration-docs"))
                .isTrue();
    }

    @Test
    void postgresEnforcesRepositoryOwnerAndNameUniqueness() {
        repositoryEntityRepository.saveAndFlush(
                new RepositoryEntity(new Repository(
                        null, "unique-owner", "unique-repo", "main",
                        "docs", true, null, null)));

        assertThrows(DataIntegrityViolationException.class, () ->
                repositoryEntityRepository.saveAndFlush(
                        new RepositoryEntity(new Repository(
                                null, "unique-owner", "unique-repo", "main",
                                "docs", true, null, null))));
    }

    @Test
    void persistsWebhookEventWithRepositoryAssociation() {
        RepositoryEntity repository = repositoryEntityRepository.saveAndFlush(
                new RepositoryEntity(new Repository(
                        null, "webhook-owner", "webhook-docs", "main",
                        "docs", true, null, null)));

        WebhookEventEntity event = webhookEventRepository.saveAndFlush(
                new WebhookEventEntity(
                        "integration-delivery-1",
                        "push",
                        "abc123",
                        "RECEIVED",
                        repository.getId()));

        assertThat(event.getId()).isNotNull();
        assertThat(webhookEventRepository.findByEventId("integration-delivery-1"))
                .isPresent()
                .get()
                .satisfies(saved -> {
                    assertThat(saved.getRepositoryId()).isEqualTo(repository.getId());
                    assertThat(saved.getCommitSha()).isEqualTo("abc123");
                });
    }
}
