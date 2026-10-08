package com.pranay.gitprasaaran.application.repository;

public class DuplicateRepositoryException extends RuntimeException {

    public DuplicateRepositoryException(String owner, String name) {
        super("Repository already registered: " + owner + "/" + name);
    }
}
