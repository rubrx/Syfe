package com.rubrangso.finance.transaction;

import com.rubrangso.finance.category.CategoryType;
import com.rubrangso.finance.transaction.dto.CreateTransactionRequest;
import com.rubrangso.finance.transaction.dto.TransactionResponse;
import com.rubrangso.finance.transaction.dto.UpdateTransactionRequest;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** CRUD endpoints for financial transactions. */
@RestController
@RequestMapping("/api/transactions")
public class TransactionController {

    private final TransactionService transactionService;

    public TransactionController(TransactionService transactionService) {
        this.transactionService = transactionService;
    }

    /**
     * Lists transactions for the current user with optional filtering.
     * All query params are optional; date params use ISO-8601 (yyyy-MM-dd).
     */
    @GetMapping
    public Map<String, List<TransactionResponse>> getAll(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) CategoryType type) {
        return Map.of("transactions",
                transactionService.getTransactions(startDate, endDate, category, type));
    }

    /** Creates a new transaction for the current user. */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TransactionResponse create(@Valid @RequestBody CreateTransactionRequest request) {
        return transactionService.createTransaction(request);
    }

    /**
     * Partially updates a transaction. {@code date} is immutable and silently ignored if sent.
     */
    @PutMapping("/{id}")
    public TransactionResponse update(
            @PathVariable Long id,
            @Valid @RequestBody UpdateTransactionRequest request) {
        return transactionService.updateTransaction(id, request);
    }

    /** Hard-deletes a transaction. */
    @DeleteMapping("/{id}")
    public Map<String, String> delete(@PathVariable Long id) {
        transactionService.deleteTransaction(id);
        return Map.of("message", "Transaction deleted successfully");
    }
}
