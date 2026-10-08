package com.rubrangso.finance.transaction;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.rubrangso.finance.category.Category;
import com.rubrangso.finance.category.CategoryRepository;
import com.rubrangso.finance.category.CategoryType;
import com.rubrangso.finance.common.exception.BusinessValidationException;
import com.rubrangso.finance.common.exception.ResourceNotFoundException;
import com.rubrangso.finance.common.security.CurrentUserProvider;
import com.rubrangso.finance.transaction.dto.CreateTransactionRequest;
import com.rubrangso.finance.transaction.dto.UpdateTransactionRequest;
import com.rubrangso.finance.user.User;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class TransactionServiceTest {

    @Mock private TransactionRepository transactionRepository;
    @Mock private CategoryRepository categoryRepository;
    @Mock private CurrentUserProvider currentUserProvider;

    private static final LocalDate TODAY = LocalDate.of(2026, 6, 15);
    private final Clock clock = Clock.fixed(
            Instant.parse("2026-06-15T00:00:00Z"), ZoneId.of("UTC"));

    private TransactionService transactionService;
    private User user;
    private Category salary;

    @BeforeEach
    void setUp() {
        transactionService = new TransactionService(
                transactionRepository, categoryRepository, currentUserProvider, clock);
        user = new User("Alice", "alice@example.com", "hashed", null);
        ReflectionTestUtils.setField(user, "id", 1L);
        salary = new Category("Salary", CategoryType.INCOME, false, null);
        ReflectionTestUtils.setField(salary, "id", 1L);
        when(currentUserProvider.getCurrentUser()).thenReturn(user);
    }

    @Test
    @DisplayName("createTransaction_validRequest_returnsResponse")
    void createTransaction_validRequest_returnsResponse() {
        var request = new CreateTransactionRequest(
                new BigDecimal("5000"), TODAY, "Salary", "January");
        var saved = new Transaction(new BigDecimal("5000.00"), TODAY, salary, "January", user);
        ReflectionTestUtils.setField(saved, "id", 1L);

        when(categoryRepository.findByNameIgnoreCaseForUser("Salary", user))
                .thenReturn(Optional.of(salary));
        when(transactionRepository.save(any(Transaction.class))).thenReturn(saved);

        var response = transactionService.createTransaction(request);

        assertThat(response.amount()).isEqualByComparingTo("5000.00");
        assertThat(response.category()).isEqualTo("Salary");
        assertThat(response.type()).isEqualTo(CategoryType.INCOME);
    }

    @Test
    @DisplayName("createTransaction_futureDate_throwsBusinessValidationException")
    void createTransaction_futureDate_throwsBusinessValidationException() {
        var request = new CreateTransactionRequest(
                new BigDecimal("100"), TODAY.plusDays(1), "Salary", null);

        assertThatThrownBy(() -> transactionService.createTransaction(request))
                .isInstanceOf(BusinessValidationException.class);
    }

    @Test
    @DisplayName("createTransaction_unknownCategory_throwsResourceNotFoundException")
    void createTransaction_unknownCategory_throwsResourceNotFoundException() {
        var request = new CreateTransactionRequest(
                new BigDecimal("100"), TODAY, "NoSuchCategory", null);
        when(categoryRepository.findByNameIgnoreCaseForUser("NoSuchCategory", user))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> transactionService.createTransaction(request))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("updateTransaction_partialFields_appliesOnlyNonNull")
    void updateTransaction_partialFields_appliesOnlyNonNull() {
        var existing = new Transaction(new BigDecimal("5000.00"), TODAY, salary, "old", user);
        ReflectionTestUtils.setField(existing, "id", 1L);
        var request = new UpdateTransactionRequest(new BigDecimal("6000"), null, "updated");

        when(transactionRepository.findByIdAndUser(1L, user)).thenReturn(Optional.of(existing));

        var response = transactionService.updateTransaction(1L, request);

        assertThat(response.amount()).isEqualByComparingTo("6000.00");
        assertThat(response.description()).isEqualTo("updated");
        assertThat(response.category()).isEqualTo("Salary"); // unchanged
    }

    @Test
    @DisplayName("updateTransaction_notFound_throwsResourceNotFoundException")
    void updateTransaction_notFound_throwsResourceNotFoundException() {
        when(transactionRepository.findByIdAndUser(99L, user)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> transactionService.updateTransaction(
                99L, new UpdateTransactionRequest(null, null, null)))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("deleteTransaction_existing_deletesIt")
    void deleteTransaction_existing_deletesIt() {
        var existing = new Transaction(new BigDecimal("5000.00"), TODAY, salary, null, user);
        ReflectionTestUtils.setField(existing, "id", 1L);
        when(transactionRepository.findByIdAndUser(1L, user)).thenReturn(Optional.of(existing));

        transactionService.deleteTransaction(1L);

        verify(transactionRepository).delete(existing);
    }

    @Test
    @DisplayName("getTransactions_delegatesToRepository")
    void getTransactions_delegatesToRepository() {
        when(transactionRepository.findWithFilters(user, null, null, null, null))
                .thenReturn(List.of());

        var result = transactionService.getTransactions(null, null, null, null);

        assertThat(result).isEmpty();
    }
}
