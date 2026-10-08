package com.pranay.gitprasaaran.config.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "gitprasaaran.security.jwt")
public record JwtProperties(
        String secret,
        long expiration,
        String issuer) {
}