package com.pranay.gitprasaaran.application.repository;

public class RepositoryNotFoundException extends RuntimeException {

    public RepositoryNotFoundException(Long repositoryId) {
        super("Repository not found: " + repositoryId);
    }
}
