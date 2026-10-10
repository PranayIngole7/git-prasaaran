package com.pranay.gitprasaaran.application.auth;

import com.pranay.gitprasaaran.infrastructure.persistence.Role;
import com.pranay.gitprasaaran.infrastructure.persistence.RoleEntity;
import com.pranay.gitprasaaran.infrastructure.persistence.RoleRepository;
import com.pranay.gitprasaaran.infrastructure.persistence.UserEntity;
import com.pranay.gitprasaaran.infrastructure.persistence.UserRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

@Service
public class RegistrationService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    public RegistrationService(
            UserRepository userRepository,
            RoleRepository roleRepository,
            PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public RegisteredUser register(String email, String password) {
        String normalizedEmail = email.trim().toLowerCase(Locale.ROOT);

        if (userRepository.existsByEmail(normalizedEmail)) {
            throw new DuplicateEmailException();
        }

        RoleEntity customerRole = roleRepository.findByName(Role.CUSTOMER)
                .orElseThrow(() -> new IllegalStateException(
                        "Required CUSTOMER role is not configured"));

        UserEntity user = new UserEntity(
                normalizedEmail,
                passwordEncoder.encode(password));
        user.addRole(customerRole);

        try {
            UserEntity saved = userRepository.saveAndFlush(user);
            return new RegisteredUser(saved.getId(), saved.getEmail());
        } catch (DataIntegrityViolationException exception) {
            throw new DuplicateEmailException();
        }
    }

    public record RegisteredUser(Long id, String email) {
    }
}
