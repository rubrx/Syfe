package com.rubrangso.finance.common.exception;

import org.springframework.http.HttpStatus;

/** Thrown when a create/update request would violate a uniqueness constraint. Maps to 409. */
public class DuplicateResourceException extends AppException {

    public DuplicateResourceException(String message) {
        super(message, HttpStatus.CONFLICT);
    }
}
