package com.pranay.gitprasaaran.application.repository;

import com.pranay.gitprasaaran.domain.repository.Repository;
import com.pranay.gitprasaaran.domain.repository.RepositoryRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RepositoryServiceTest {

    @Mock
    private RepositoryRepository repositoryRepository;

    @InjectMocks
    private RepositoryService repositoryService;

    @Test
    void shouldCreateRepositoryWithGeneratedIdFromPersistence() {
        when(repositoryRepository.existsByOwnerAndName("octo", "docs")).thenReturn(false);
        when(repositoryRepository.save(any(Repository.class))).thenReturn(repository(8L, true));

        Repository result = repositoryService.create("octo", "docs", "main", "documentation");

        assertEquals(8L, result.id());
        verify(repositoryRepository).save(any(Repository.class));
    }

    @Test
    void shouldRejectDuplicateBeforeSaving() {
        when(repositoryRepository.existsByOwnerAndName("octo", "docs")).thenReturn(true);

        assertThrows(
                DuplicateRepositoryException.class,
                () -> repositoryService.create("octo", "docs", "main", "documentation")
        );
    }

    @Test
    void shouldApplyPartialUpdatesAndDeactivation() {
        when(repositoryRepository.findById(1L)).thenReturn(Optional.of(repository(1L, true)));
        when(repositoryRepository.save(any(Repository.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Repository updated = repositoryService.update(1L, null, null, "release", null, false);

        assertEquals("release", updated.branch());
        assertFalse(updated.active());
        assertEquals("octo", updated.owner());
    }

    @Test
    void shouldRejectDuplicateIdentityWhenUpdating() {
        when(repositoryRepository.findById(1L)).thenReturn(Optional.of(repository(1L, true)));
        when(repositoryRepository.existsByOwnerAndName("another", "docs")).thenReturn(true);

        assertThrows(
                DuplicateRepositoryException.class,
                () -> repositoryService.update(1L, "another", null, null, null, null)
        );
    }

    @Test
    void shouldFailWhenUpdatingUnknownRepository() {
        when(repositoryRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(
                RepositoryNotFoundException.class,
                () -> repositoryService.update(999L, null, null, null, null, false)
        );
    }

    private static Repository repository(Long id, boolean active) {
        Instant instant = Instant.parse("2026-01-01T00:00:00Z");
        return new Repository(id, "octo", "docs", "main", "documentation", active, instant, instant);
    }
}
