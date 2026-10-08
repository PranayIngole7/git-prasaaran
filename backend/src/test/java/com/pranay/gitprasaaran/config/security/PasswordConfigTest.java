package com.pranay.gitprasaaran.config.security;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.*;

class PasswordConfigTest {

    private final PasswordEncoder passwordEncoder = new PasswordConfig().passwordEncoder();

    @Test
    void shouldHashPasswordAndMatchOriginal() {
        String rawPassword = "correct-password";

        String encodedPassword = passwordEncoder.encode(rawPassword);

        assertNotEquals(rawPassword, encodedPassword);
        assertTrue(passwordEncoder.matches(rawPassword, encodedPassword));
    }

    @Test
    void shouldRejectIncorrectPassword() {
        String encodedPassword = passwordEncoder.encode("correct-password");

        assertFalse(
                passwordEncoder.matches("wrong-password", encodedPassword));
    }
}