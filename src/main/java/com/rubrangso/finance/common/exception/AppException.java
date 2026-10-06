package com.rubrangso.finance.common.exception;

import org.springframework.http.HttpStatus;

/**
 * Base class for all domain exceptions. Each concrete subclass embeds the {@link HttpStatus}
 * it maps to, allowing a single {@code @ExceptionHandler(AppException.class)} to handle the
 * entire hierarchy without per-exception handler methods.
 */
public abstract class AppException extends RuntimeException {

    private final HttpStatus httpStatus;

    /**
     * @param message human-readable description included in the error response body
     * @param httpStatus HTTP status code this exception maps to
     */
    protected AppException(String message, HttpStatus httpStatus) {
        super(message);
        this.httpStatus = httpStatus;
    }

    /** Returns the HTTP status code that {@link GlobalExceptionHandler} should use. */
    public HttpStatus getHttpStatus() {
        return httpStatus;
    }
}
