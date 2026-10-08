package com.rubrangso.finance.transaction.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import java.time.LocalDate;

public record CreateTransactionRequest(

        @NotNull(message = "Amount is required")
        @Positive(message = "Amount must be greater than 0")
        BigDecimal amount,

        @NotNull(message = "Date is required")
        LocalDate date,

        @NotBlank(message = "Category is required")
        String category,

        String description
) {}
