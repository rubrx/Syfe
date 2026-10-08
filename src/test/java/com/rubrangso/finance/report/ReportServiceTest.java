package com.rubrangso.finance.report;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.rubrangso.finance.category.CategoryType;
import com.rubrangso.finance.common.exception.BusinessValidationException;
import com.rubrangso.finance.common.security.CurrentUserProvider;
import com.rubrangso.finance.transaction.TransactionRepository;
import com.rubrangso.finance.user.User;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ReportServiceTest {

    @Mock private TransactionRepository transactionRepository;
    @Mock private CurrentUserProvider currentUserProvider;

    private ReportService reportService;
    private User user;

    @BeforeEach
    void setUp() {
        reportService = new ReportService(transactionRepository, currentUserProvider);
        user = new User("Alice", "alice@example.com", "hashed", null);
        ReflectionTestUtils.setField(user, "id", 1L);
        when(currentUserProvider.getCurrentUser()).thenReturn(user);
    }

    private static List<Object[]> rows(Object[]... arrays) {
        var list = new ArrayList<Object[]>();
        for (var arr : arrays) list.add(arr);
        return list;
    }

    @Test
    @DisplayName("getMonthlyReport_emptyPeriod_returnsEmptyMapsAndZeroNetSavings")
    void getMonthlyReport_emptyPeriod_returnsEmptyMapsAndZeroNetSavings() {
        when(transactionRepository.sumByCategoryForUserAndType(any(), any(), any(), any()))
                .thenReturn(new ArrayList<>());

        var result = reportService.getMonthlyReport(2026, 1);

        assertThat(result.totalIncome()).isEmpty();
        assertThat(result.totalExpenses()).isEmpty();
        assertThat(result.netSavings()).isEqualByComparingTo("0.00");
        assertThat(result.month()).isEqualTo(1);
        assertThat(result.year()).isEqualTo(2026);
    }

    @Test
    @DisplayName("getMonthlyReport_withTransactions_returnsCorrectMapsAndNetSavings")
    void getMonthlyReport_withTransactions_returnsCorrectMapsAndNetSavings() {
        var jan = LocalDate.of(2026, 1, 1);
        var jan31 = LocalDate.of(2026, 1, 31);

        when(transactionRepository.sumByCategoryForUserAndType(
                eq(user), eq(CategoryType.INCOME), eq(jan), eq(jan31)))
                .thenReturn(rows(
                        new Object[]{"Salary", new BigDecimal("3000.00")},
                        new Object[]{"Freelance", new BigDecimal("500.00")}));

        when(transactionRepository.sumByCategoryForUserAndType(
                eq(user), eq(CategoryType.EXPENSE), eq(jan), eq(jan31)))
                .thenReturn(rows(
                        new Object[]{"Food", new BigDecimal("400.00")},
                        new Object[]{"Rent", new BigDecimal("1200.00")}));

        var result = reportService.getMonthlyReport(2026, 1);

        assertThat(result.totalIncome()).containsEntry("Salary", new BigDecimal("3000.00"))
                .containsEntry("Freelance", new BigDecimal("500.00"));
        assertThat(result.totalExpenses()).containsEntry("Food", new BigDecimal("400.00"))
                .containsEntry("Rent", new BigDecimal("1200.00"));
        assertThat(result.netSavings()).isEqualByComparingTo("1900.00");
    }

    @Test
    @DisplayName("getMonthlyReport_invalidMonth_throwsBusinessValidationException")
    void getMonthlyReport_invalidMonth_throwsBusinessValidationException() {
        assertThatThrownBy(() -> reportService.getMonthlyReport(2026, 13))
                .isInstanceOf(BusinessValidationException.class);

        assertThatThrownBy(() -> reportService.getMonthlyReport(2026, 0))
                .isInstanceOf(BusinessValidationException.class);
    }

    @Test
    @DisplayName("getMonthlyReport_invalidYear_throwsBusinessValidationException")
    void getMonthlyReport_invalidYear_throwsBusinessValidationException() {
        assertThatThrownBy(() -> reportService.getMonthlyReport(1899, 6))
                .isInstanceOf(BusinessValidationException.class);

        assertThatThrownBy(() -> reportService.getMonthlyReport(2101, 6))
                .isInstanceOf(BusinessValidationException.class);
    }

    @Test
    @DisplayName("getYearlyReport_emptyPeriod_returnsEmptyMapsAndZeroNetSavings")
    void getYearlyReport_emptyPeriod_returnsEmptyMapsAndZeroNetSavings() {
        when(transactionRepository.sumByCategoryForUserAndType(any(), any(), any(), any()))
                .thenReturn(new ArrayList<>());

        var result = reportService.getYearlyReport(2025);

        assertThat(result.totalIncome()).isEmpty();
        assertThat(result.totalExpenses()).isEmpty();
        assertThat(result.netSavings()).isEqualByComparingTo("0.00");
        assertThat(result.year()).isEqualTo(2025);
    }

    @Test
    @DisplayName("getYearlyReport_withTransactions_returnsCorrectAggregation")
    void getYearlyReport_withTransactions_returnsCorrectAggregation() {
        var startOfYear = LocalDate.of(2025, 1, 1);
        var endOfYear = LocalDate.of(2025, 12, 31);

        when(transactionRepository.sumByCategoryForUserAndType(
                eq(user), eq(CategoryType.INCOME), eq(startOfYear), eq(endOfYear)))
                .thenReturn(rows(new Object[]{"Salary", new BigDecimal("36000.00")}));

        when(transactionRepository.sumByCategoryForUserAndType(
                eq(user), eq(CategoryType.EXPENSE), eq(startOfYear), eq(endOfYear)))
                .thenReturn(rows(new Object[]{"Rent", new BigDecimal("14400.00")}));

        var result = reportService.getYearlyReport(2025);

        assertThat(result.totalIncome()).containsEntry("Salary", new BigDecimal("36000.00"));
        assertThat(result.totalExpenses()).containsEntry("Rent", new BigDecimal("14400.00"));
        assertThat(result.netSavings()).isEqualByComparingTo("21600.00");
    }

    @Test
    @DisplayName("getYearlyReport_invalidYear_throwsBusinessValidationException")
    void getYearlyReport_invalidYear_throwsBusinessValidationException() {
        assertThatThrownBy(() -> reportService.getYearlyReport(1899))
                .isInstanceOf(BusinessValidationException.class);
    }
}
