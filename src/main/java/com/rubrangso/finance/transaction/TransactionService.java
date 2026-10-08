package com.rubrangso.finance.transaction;

import com.rubrangso.finance.category.Category;
import com.rubrangso.finance.category.CategoryRepository;
import com.rubrangso.finance.category.CategoryType;
import com.rubrangso.finance.common.exception.BusinessValidationException;
import com.rubrangso.finance.common.exception.ResourceNotFoundException;
import com.rubrangso.finance.common.security.CurrentUserProvider;
import com.rubrangso.finance.user.User;
import com.rubrangso.finance.transaction.dto.CreateTransactionRequest;
import com.rubrangso.finance.transaction.dto.TransactionResponse;
import com.rubrangso.finance.transaction.dto.UpdateTransactionRequest;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Business logic for transaction management.
 * Dates are validated against an injected {@link Clock} bean for deterministic testing.
 * Money values are always rounded to scale 2 with {@link RoundingMode#HALF_UP}.
 */
@Service
@Transactional(readOnly = true)
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final CategoryRepository categoryRepository;
    private final CurrentUserProvider currentUserProvider;
    private final Clock clock;

    public TransactionService(
            TransactionRepository transactionRepository,
            CategoryRepository categoryRepository,
            CurrentUserProvider currentUserProvider,
            Clock clock) {
        this.transactionRepository = transactionRepository;
        this.categoryRepository = categoryRepository;
        this.currentUserProvider = currentUserProvider;
        this.clock = clock;
    }

    /**
     * Returns the current user's transactions matching all provided filters.
     * All filter parameters are optional; null means no constraint.
     */
    public List<TransactionResponse> getTransactions(
            LocalDate startDate, LocalDate endDate, String category, CategoryType type) {
        var user = currentUserProvider.getCurrentUser();
        return transactionRepository
                .findWithFilters(user, startDate, endDate, category, type)
                .stream()
                .map(TransactionResponse::from)
                .toList();
    }

    /**
     * Creates a transaction. The category is resolved by name (case-insensitive).
     *
     * @throws BusinessValidationException if {@code date} is in the future
     * @throws ResourceNotFoundException if no accessible category matches the given name
     */
    @Transactional
    public TransactionResponse createTransaction(CreateTransactionRequest request) {
        var user = currentUserProvider.getCurrentUser();
        validateNotFuture(request.date());

        var category = resolveCategory(request.category(), user);
        var amount = request.amount().setScale(2, RoundingMode.HALF_UP);
        var transaction = new Transaction(amount, request.date(), category, request.description(), user);
        return TransactionResponse.from(transactionRepository.save(transaction));
    }

    /**
     * Partially updates a transaction. Only non-null fields are applied.
     * {@code date} is immutable — not present in {@link UpdateTransactionRequest}.
     *
     * @throws ResourceNotFoundException if the transaction does not exist or belongs to another user
     */
    @Transactional
    public TransactionResponse updateTransaction(Long id, UpdateTransactionRequest request) {
        var user = currentUserProvider.getCurrentUser();
        var transaction = transactionRepository.findByIdAndUser(id, user)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction not found: id=" + id));

        if (request.amount() != null) {
            transaction.setAmount(request.amount().setScale(2, RoundingMode.HALF_UP));
        }
        if (request.category() != null) {
            transaction.setCategory(resolveCategory(request.category(), user));
        }
        if (request.description() != null) {
            transaction.setDescription(request.description());
        }

        return TransactionResponse.from(transaction);
    }

    /**
     * Hard-deletes a transaction.
     *
     * @throws ResourceNotFoundException if the transaction does not exist or belongs to another user
     */
    @Transactional
    public void deleteTransaction(Long id) {
        var user = currentUserProvider.getCurrentUser();
        var transaction = transactionRepository.findByIdAndUser(id, user)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction not found: id=" + id));
        transactionRepository.delete(transaction);
    }

    private Category resolveCategory(String name, User user) {
        return categoryRepository.findByNameIgnoreCaseForUser(name, user)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Category '" + name + "' not found"));
    }

    private void validateNotFuture(LocalDate date) {
        if (date.isAfter(LocalDate.now(clock))) {
            throw new BusinessValidationException("Transaction date cannot be in the future");
        }
    }

}
