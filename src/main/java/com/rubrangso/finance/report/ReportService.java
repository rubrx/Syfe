package com.rubrangso.finance.report;

import com.rubrangso.finance.category.CategoryType;
import com.rubrangso.finance.common.exception.BusinessValidationException;
import com.rubrangso.finance.common.security.CurrentUserProvider;
import com.rubrangso.finance.report.dto.MonthlyReportResponse;
import com.rubrangso.finance.report.dto.YearlyReportResponse;
import com.rubrangso.finance.transaction.TransactionRepository;
import com.rubrangso.finance.user.User;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Produces income/expense summaries grouped by category.
 * All aggregation is done in the database via GROUP BY; no Java-side looping over transactions.
 */
@Service
@Transactional(readOnly = true)
public class ReportService {

    private final TransactionRepository transactionRepository;
    private final CurrentUserProvider currentUserProvider;

    public ReportService(TransactionRepository transactionRepository,
            CurrentUserProvider currentUserProvider) {
        this.transactionRepository = transactionRepository;
        this.currentUserProvider = currentUserProvider;
    }

    /**
     * Returns income and expense totals grouped by category for a single calendar month.
     *
     * @throws BusinessValidationException if month is not 1–12 or year is not 1900–2100
     */
    public MonthlyReportResponse getMonthlyReport(int year, int month) {
        validateYear(year);
        if (month < 1 || month > 12) {
            throw new BusinessValidationException("month must be between 1 and 12");
        }

        var user = currentUserProvider.getCurrentUser();
        var startDate = LocalDate.of(year, month, 1);
        var endDate = startDate.with(TemporalAdjusters.lastDayOfMonth());

        var income = buildCategoryMap(user, CategoryType.INCOME, startDate, endDate);
        var expenses = buildCategoryMap(user, CategoryType.EXPENSE, startDate, endDate);
        var net = sumValues(income).subtract(sumValues(expenses)).setScale(2, RoundingMode.HALF_UP);

        return new MonthlyReportResponse(month, year, income, expenses, net);
    }

    /**
     * Returns income and expense totals grouped by category for a full calendar year.
     *
     * @throws BusinessValidationException if year is not 1900–2100
     */
    public YearlyReportResponse getYearlyReport(int year) {
        validateYear(year);

        var user = currentUserProvider.getCurrentUser();
        var startDate = LocalDate.of(year, 1, 1);
        var endDate = LocalDate.of(year, 12, 31);

        var income = buildCategoryMap(user, CategoryType.INCOME, startDate, endDate);
        var expenses = buildCategoryMap(user, CategoryType.EXPENSE, startDate, endDate);
        var net = sumValues(income).subtract(sumValues(expenses)).setScale(2, RoundingMode.HALF_UP);

        return new YearlyReportResponse(year, income, expenses, net);
    }

    private void validateYear(int year) {
        if (year < 1900 || year > 2100) {
            throw new BusinessValidationException("year must be between 1900 and 2100");
        }
    }

    private Map<String, BigDecimal> buildCategoryMap(User user, CategoryType type,
            LocalDate startDate, LocalDate endDate) {
        List<Object[]> rows = transactionRepository
                .sumByCategoryForUserAndType(user, type, startDate, endDate);
        var map = new LinkedHashMap<String, BigDecimal>();
        for (Object[] row : rows) {
            var name = (String) row[0];
            var total = ((BigDecimal) row[1]).setScale(2, RoundingMode.HALF_UP);
            map.put(name, total);
        }
        return map;
    }

    private BigDecimal sumValues(Map<String, BigDecimal> map) {
        return map.values().stream()
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
