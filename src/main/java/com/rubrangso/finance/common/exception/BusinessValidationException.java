package com.rubrangso.finance.common.exception;

import org.springframework.http.HttpStatus;

/**
 * Thrown for domain rule violations that are not covered by Bean Validation constraints
 * (e.g. attempting to delete a category that is still referenced by transactions). Maps to 400.
 */
public class BusinessValidationException extends AppException {

    public BusinessValidationException(String message) {
        super(message, HttpStatus.BAD_REQUEST);
    }
}
