package com.pranay.gitprasaaran.application.document;

public class PrivateDocumentNotFoundException extends RuntimeException {

    public PrivateDocumentNotFoundException() {
        super("Private document not found");
    }
}
