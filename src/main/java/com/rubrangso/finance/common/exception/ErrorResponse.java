package com.rubrangso.finance.common.exception;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.http.HttpStatus;

/**
 * Uniform JSON error envelope returned by {@link GlobalExceptionHandler} for every non-2xx
 * response. {@code fieldErrors} is present only on Bean Validation failures; it is omitted
 * from the serialised JSON when {@code null} via {@link JsonInclude}.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ErrorResponse(
        LocalDateTime timestamp,
        int status,
        String error,
        String message,
        String path,
        List<FieldErrorDetail> fieldErrors) {

    /** A single Bean Validation violation with the field name and constraint message. */
    public record FieldErrorDetail(String field, String message) {}

    /**
     * Builds an error response without field-level details (all non-validation errors).
     *
     * @param status HTTP status to report
     * @param message human-readable description
     * @param path request URI
     * @return a fully populated {@code ErrorResponse} with {@code fieldErrors} omitted
     */
    public static ErrorResponse of(HttpStatus status, String message, String path) {
        return new ErrorResponse(
                LocalDateTime.now(), status.value(), status.getReasonPhrase(), message, path, null);
    }

    /**
     * Builds an error response with field-level validation details.
     *
     * @param status HTTP status to report (typically 400)
     * @param message top-level description
     * @param path request URI
     * @param fieldErrors per-field constraint violations
     * @return a fully populated {@code ErrorResponse}
     */
    public static ErrorResponse of(
            HttpStatus status,
            String message,
            String path,
            List<FieldErrorDetail> fieldErrors) {
        return new ErrorResponse(
                LocalDateTime.now(),
                status.value(),
                status.getReasonPhrase(),
                message,
                path,
                fieldErrors);
    }
}
