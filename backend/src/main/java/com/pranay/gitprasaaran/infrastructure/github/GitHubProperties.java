package com.pranay.gitprasaaran.infrastructure.github;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "gitprasaaran.github")
public record GitHubProperties(
        String owner,
        String repository,
        String branch,
        String contentPath,
        String token
) {
}
