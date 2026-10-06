package com.rubrangso.finance.common.exception;

import org.springframework.http.HttpStatus;

/** Thrown when a requested resource does not exist or is not visible to the caller. Maps to 404. */
public class ResourceNotFoundException extends AppException {

    public ResourceNotFoundException(String message) {
        super(message, HttpStatus.NOT_FOUND);
    }
}
