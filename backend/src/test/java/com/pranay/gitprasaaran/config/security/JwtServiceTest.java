package com.pranay.gitprasaaran.config.security;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class JwtServiceTest {

    private static final String SECRET =
            "test-secret-key-for-jwt-signing-at-least-32-bytes";
    private static final String ISSUER = "git-prasaaran";
    private static final String USERNAME = "customer@example.com";

    private JwtService jwtService;
    private UserDetails userDetails;
    private SecretKey signingKey;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService(new JwtProperties(SECRET, 900, ISSUER));
        signingKey = Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));
        userDetails = User.withUsername(USERNAME)
                .password("not-used")
                .roles("CUSTOMER")
                .build();
    }

    @Test
    void shouldGenerateAndValidateTokenWithConfiguredIssuer() {
        String token = jwtService.generateToken(userDetails);
        assertEquals(USERNAME, jwtService.extractUsername(token));
        assertTrue(jwtService.isTokenValid(token, userDetails));
    }

    @Test
    void shouldRejectTokenWithDifferentIssuer() {
        String token = createToken("unexpected-issuer");
        assertThrows(RuntimeException.class, () -> jwtService.extractUsername(token));
        assertFalse(jwtService.isTokenValid(token, userDetails));
    }

    @Test
    void shouldRejectExpiredToken() {
        String token = Jwts.builder()
                .subject(USERNAME)
                .issuer(ISSUER)
                .issuedAt(Date.from(Instant.now().minusSeconds(120)))
                .expiration(Date.from(Instant.now().minusSeconds(60)))
                .claim("roles", List.of("ROLE_CUSTOMER"))
                .signWith(signingKey)
                .compact();

        assertFalse(jwtService.isTokenValid(token, userDetails));
    }

    @Test
    void shouldRejectTokenForDifferentUsername() {
        String token = jwtService.generateToken(userDetails);
        UserDetails anotherUser = User.withUsername("another@example.com")
                .password("not-used")
                .roles("CUSTOMER")
                .build();

        assertFalse(jwtService.isTokenValid(token, anotherUser));
    }

    private String createToken(String issuer) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(USERNAME)
                .issuer(issuer)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusSeconds(900)))
                .claim("roles", List.of("ROLE_CUSTOMER"))
                .signWith(signingKey)
                .compact();
    }
}
