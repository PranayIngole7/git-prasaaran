package com.pranay.gitprasaaran.api.error;

import com.pranay.gitprasaaran.application.activity.InvalidActivityQueryException;
import com.pranay.gitprasaaran.application.repository.DuplicateRepositoryException;
import com.pranay.gitprasaaran.application.repository.RepositoryNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(RepositoryNotFoundException.class)
    public ResponseEntity<ApiError> handleRepositoryNotFound(RepositoryNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ApiError("REPOSITORY_NOT_FOUND", "Repository not found"));
    }

    @ExceptionHandler(DuplicateRepositoryException.class)
    public ResponseEntity<ApiError> handleDuplicateRepository(DuplicateRepositoryException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new ApiError("REPOSITORY_ALREADY_EXISTS", "Repository is already registered"));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiError handleInvalidRequest(MethodArgumentNotValidException ex) {
        return new ApiError("INVALID_REQUEST", "Request validation failed");
    }

    @ExceptionHandler(InvalidActivityQueryException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiError handleInvalidActivityQuery(InvalidActivityQueryException ex) {
        return new ApiError("INVALID_ACTIVITY_QUERY", ex.getMessage());
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiError handleInvalidParameter(MethodArgumentTypeMismatchException ex) {
        return new ApiError("INVALID_REQUEST", "Request parameter is invalid");
    }

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
