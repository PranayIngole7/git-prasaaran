package com.pranay.gitprasaaran.application.auth;

import com.pranay.gitprasaaran.infrastructure.persistence.Role;
import com.pranay.gitprasaaran.infrastructure.persistence.RoleEntity;
import com.pranay.gitprasaaran.infrastructure.persistence.RoleRepository;
import com.pranay.gitprasaaran.infrastructure.persistence.UserEntity;
import com.pranay.gitprasaaran.infrastructure.persistence.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RegistrationServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private RoleEntity customerRole;

    @InjectMocks
    private RegistrationService registrationService;

    @Test
    void shouldNormalizeEmailHashPasswordAndAssignCustomerRole() {
        when(roleRepository.findByName(Role.CUSTOMER))
                .thenReturn(Optional.of(customerRole));
        when(passwordEncoder.encode("securePassword123"))
                .thenReturn("bcrypt-hash");
        when(userRepository.existsByEmail("customer@example.com"))
                .thenReturn(false);
        when(userRepository.saveAndFlush(any(UserEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        RegistrationService.RegisteredUser result =
                registrationService.register(
                        "  Customer@Example.com  ",
                        "securePassword123");

        ArgumentCaptor<UserEntity> captor =
                ArgumentCaptor.forClass(UserEntity.class);
        verify(userRepository).saveAndFlush(captor.capture());

        UserEntity savedUser = captor.getValue();

        assertEquals("customer@example.com", savedUser.getEmail());
        assertEquals("bcrypt-hash", savedUser.getPasswordHash());
        assertTrue(savedUser.isEnabled());
        assertTrue(savedUser.getRoles().contains(customerRole));
        assertEquals("customer@example.com", result.email());

        verify(userRepository).existsByEmail("customer@example.com");
        verify(passwordEncoder).encode("securePassword123");
        verify(roleRepository).findByName(Role.CUSTOMER);
    }

    @Test
    void shouldRejectAnAlreadyRegisteredEmail() {
        when(userRepository.existsByEmail("customer@example.com"))
                .thenReturn(true);

        assertThrows(
                DuplicateEmailException.class,
                () -> registrationService.register(
                        "Customer@Example.com",
                        "securePassword123"));

        verify(userRepository, never()).saveAndFlush(any());
        verifyNoInteractions(passwordEncoder);
        verifyNoInteractions(roleRepository);
    }

    @Test
    void shouldTranslateConcurrentDuplicateInsertIntoDuplicateEmailException() {
        when(userRepository.existsByEmail("customer@example.com"))
                .thenReturn(false);
        when(roleRepository.findByName(Role.CUSTOMER))
                .thenReturn(Optional.of(customerRole));
        when(passwordEncoder.encode("securePassword123"))
                .thenReturn("bcrypt-hash");
        when(userRepository.saveAndFlush(any(UserEntity.class)))
                .thenThrow(new DataIntegrityViolationException("Duplicate email"));

        assertThrows(
                DuplicateEmailException.class,
                () -> registrationService.register(
                        "customer@example.com",
                        "securePassword123"));
    }

    @Test
    void shouldFailIfCustomerRoleIsNotConfigured() {
        when(userRepository.existsByEmail("customer@example.com"))
                .thenReturn(false);
        when(roleRepository.findByName(Role.CUSTOMER))
                .thenReturn(Optional.empty());

        assertThrows(
                IllegalStateException.class,
                () -> registrationService.register(
                        "customer@example.com",
                        "securePassword123"));

        verify(userRepository, never()).saveAndFlush(any());
        verifyNoInteractions(passwordEncoder);
    }
}
