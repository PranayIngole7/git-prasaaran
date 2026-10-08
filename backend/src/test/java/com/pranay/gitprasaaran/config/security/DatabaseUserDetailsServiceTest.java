package com.pranay.gitprasaaran.config.security;

import com.pranay.gitprasaaran.infrastructure.persistence.RoleEntity;
import com.pranay.gitprasaaran.infrastructure.persistence.UserEntity;
import com.pranay.gitprasaaran.infrastructure.persistence.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Optional;
import java.util.Set;
import com.pranay.gitprasaaran.infrastructure.persistence.Role;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class DatabaseUserDetailsServiceTest {

    private final UserRepository userRepository = mock(UserRepository.class);

    private final DatabaseUserDetailsService userDetailsService = new DatabaseUserDetailsService(userRepository);

    @Test
    void shouldLoadEnabledUserByEmail() {
        UserEntity user = new UserEntity("user@example.com", "hashed-password");

        when(userRepository.findByEmail("user@example.com"))
                .thenReturn(Optional.of(user));

        UserDetails result = userDetailsService.loadUserByUsername("user@example.com");

        assertEquals("user@example.com", result.getUsername());
        assertEquals("hashed-password", result.getPassword());
        assertTrue(result.isEnabled());

        verify(userRepository).findByEmail("user@example.com");
    }

    @Test
    void shouldRejectUnknownUser() {
        when(userRepository.findByEmail("missing@example.com"))
                .thenReturn(Optional.empty());

        assertThrows(
                UsernameNotFoundException.class,
                () -> userDetailsService.loadUserByUsername(
                        "missing@example.com"));

        verify(userRepository).findByEmail("missing@example.com");
    }

    @Test
    void shouldReturnDisabledUserAsDisabled() {
        UserEntity user = new UserEntity("disabled@example.com", "hashed-password");

        user.disable();

        when(userRepository.findByEmail("disabled@example.com"))
                .thenReturn(Optional.of(user));

        UserDetails result = userDetailsService.loadUserByUsername(
                "disabled@example.com");

        assertFalse(result.isEnabled());
    }

    @Test
    void shouldLoadCustomerRoleAsAuthority() {
        UserEntity user = new UserEntity("customer@example.com", "hashed-password");

        user.addRole(new RoleEntity(Role.CUSTOMER));

        when(userRepository.findByEmail("customer@example.com"))
                .thenReturn(Optional.of(user));

        UserDetails result = userDetailsService.loadUserByUsername("customer@example.com");

        assertTrue(result.getAuthorities().stream()
                .anyMatch(authority -> authority.getAuthority().equals("ROLE_CUSTOMER")));
    }

    @Test
    void shouldLoadMultipleRolesAsAuthorities() {
        UserEntity user = new UserEntity("admin@example.com", "hashed-password");

        user.addRole(new RoleEntity(Role.SUPPORT));
        user.addRole(new RoleEntity(Role.ADMIN));

        when(userRepository.findByEmail("admin@example.com"))
                .thenReturn(Optional.of(user));

        UserDetails result = userDetailsService.loadUserByUsername("admin@example.com");

        Set<String> authorities = result.getAuthorities().stream()
                .map(a -> a.getAuthority())
                .collect(java.util.stream.Collectors.toSet());

        assertEquals(
                Set.of("ROLE_SUPPORT", "ROLE_ADMIN"),
                authorities);
    }
}