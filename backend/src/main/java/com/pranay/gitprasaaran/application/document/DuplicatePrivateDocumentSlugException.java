package com.pranay.gitprasaaran.application.document;

public class DuplicatePrivateDocumentSlugException extends RuntimeException {

    public DuplicatePrivateDocumentSlugException() {
        super("Private document slug already exists");
    }
}
