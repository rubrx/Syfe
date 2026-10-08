package com.rubrangso.finance.transaction.dto;

import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;

/**
 * Partial update — every field is optional (null = unchanged).
 * {@code date} is intentionally absent; if sent in the body Jackson silently discards it.
 */
public record UpdateTransactionRequest(

        @Positive(message = "Amount must be greater than 0")
        BigDecimal amount,

        String category,

        String description
) {}
