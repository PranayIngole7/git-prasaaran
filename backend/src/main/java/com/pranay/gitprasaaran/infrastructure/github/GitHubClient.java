package com.pranay.gitprasaaran.infrastructure.github;

import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

@Component
public class GitHubClient {

    private final WebClient webClient;
    private final GitHubProperties properties;

    public GitHubClient(GitHubProperties properties) {
        this.properties = properties;
        this.webClient = WebClient.builder()
                .baseUrl("https://api.github.com")
                .defaultHeader("Accept", "application/vnd.github+json")
                .build();
    }

    public String getFile(String path) {
        return getFile(
                properties.owner(),
                properties.repository(),
                properties.branch(),
                path
        );
    }

    public String getFile(String owner, String repository, String branch, String path) {
        return webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/repos/{owner}/{repo}/contents/{path}")
                        .queryParam("ref", branch)
                        .build(
                                owner,
                                repository,
                                path
                        ))
                .headers(headers -> applyAuthorization(headers))
                .retrieve()
                .bodyToMono(String.class)
                .block();
    }

    public String listDirectory(String path) {
        return listDirectory(
                properties.owner(),
                properties.repository(),
                properties.branch(),
                path
        );
    }

    public String listDirectory(String owner, String repository, String branch, String path) {
        return webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/repos/{owner}/{repo}/contents/{path}")
                        .queryParam("ref", branch)
                        .build(
                                owner,
                                repository,
                                path
                        ))
                .headers(headers -> applyAuthorization(headers))
                .retrieve()
                .bodyToMono(String.class)
                .block();
    }

    private void applyAuthorization(
            org.springframework.http.HttpHeaders headers
    ) {
        if (properties.token() != null && !properties.token().isBlank()) {
            headers.setBearerAuth(properties.token());
        }
    }
}
