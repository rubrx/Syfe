package com.rubrangso.finance.common.exception;

import org.springframework.http.HttpStatus;

/**
 * Thrown when the caller is authenticated but lacks permission for the requested operation
 * (e.g. attempting to delete a system-default category). Maps to 403.
 */
public class ForbiddenOperationException extends AppException {

    public ForbiddenOperationException(String message) {
        super(message, HttpStatus.FORBIDDEN);
    }
}
