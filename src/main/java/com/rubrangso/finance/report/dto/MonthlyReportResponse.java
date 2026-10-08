package com.rubrangso.finance.report.dto;

import java.math.BigDecimal;
import java.util.Map;

/** Response for GET /api/reports/monthly/{year}/{month}. */
public record MonthlyReportResponse(
        int month,
        int year,
        Map<String, BigDecimal> totalIncome,
        Map<String, BigDecimal> totalExpenses,
        BigDecimal netSavings) {}
