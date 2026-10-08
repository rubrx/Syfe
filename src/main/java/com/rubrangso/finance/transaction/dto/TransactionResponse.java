package com.rubrangso.finance.transaction.dto;

import com.rubrangso.finance.category.CategoryType;
import com.rubrangso.finance.transaction.Transaction;
import java.math.BigDecimal;
import java.time.LocalDate;

public record TransactionResponse(
        Long id,
        BigDecimal amount,
        LocalDate date,
        String category,
        String description,
        CategoryType type) {

    public static TransactionResponse from(Transaction t) {
        return new TransactionResponse(
                t.getId(),
                t.getAmount(),
                t.getDate(),
                t.getCategory().getName(),
                t.getDescription(),
                t.getType());
    }
}
