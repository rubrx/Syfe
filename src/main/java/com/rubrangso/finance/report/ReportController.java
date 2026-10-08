package com.rubrangso.finance.report;

import com.rubrangso.finance.report.dto.MonthlyReportResponse;
import com.rubrangso.finance.report.dto.YearlyReportResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST endpoints for financial reports.
 * All endpoints require authentication.
 */
@RestController
@RequestMapping("/api/reports")
public class ReportController {

    private final ReportService reportService;

    public ReportController(ReportService reportService) {
        this.reportService = reportService;
    }

    /** Returns income and expense totals grouped by category for the given month. */
    @GetMapping("/monthly/{year}/{month}")
    public MonthlyReportResponse getMonthlyReport(
            @PathVariable int year,
            @PathVariable int month) {
        return reportService.getMonthlyReport(year, month);
    }

    /** Returns income and expense totals grouped by category for the given year. */
    @GetMapping("/yearly/{year}")
    public YearlyReportResponse getYearlyReport(@PathVariable int year) {
        return reportService.getYearlyReport(year);
    }
}
