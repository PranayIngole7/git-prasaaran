package com.pranay.gitprasaaran.api.error;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(IllegalStateException.class)
    @ResponseStatus(HttpStatus.BAD_GATEWAY)
    public ApiError handleIllegalStateException(IllegalStateException ex) {
        return new ApiError(
                "DOCUMENT_RETRIEVAL_FAILED",
                "Unable to retrieve document"
        );
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ApiError handleUnexpectedException(Exception ex) {
        return new ApiError(
                "INTERNAL_ERROR",
                "An unexpected error occurred"
        );
    }
}
