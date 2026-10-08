package com.rubrangso.finance.report;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.rubrangso.finance.common.exception.BusinessValidationException;
import com.rubrangso.finance.common.exception.GlobalExceptionHandler;
import com.rubrangso.finance.config.SecurityConfig;
import com.rubrangso.finance.report.dto.MonthlyReportResponse;
import com.rubrangso.finance.report.dto.YearlyReportResponse;
import com.rubrangso.finance.user.AppUserDetailsService;
import java.math.BigDecimal;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(ReportController.class)
@Import({SecurityConfig.class, GlobalExceptionHandler.class})
@WithMockUser
class ReportControllerTest {

    @Autowired MockMvc mockMvc;

    @MockitoBean ReportService reportService;
    @MockitoBean AppUserDetailsService appUserDetailsService;

    @Test
    @DisplayName("GET /api/reports/monthly/{year}/{month} returns 200 with report")
    void getMonthly_validParams_returns200() throws Exception {
        var response = new MonthlyReportResponse(6, 2026,
                Map.of("Salary", new BigDecimal("3000.00")),
                Map.of("Food", new BigDecimal("400.00")),
                new BigDecimal("2600.00"));
        when(reportService.getMonthlyReport(2026, 6)).thenReturn(response);

        mockMvc.perform(get("/api/reports/monthly/2026/6"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.month").value(6))
                .andExpect(jsonPath("$.year").value(2026))
                .andExpect(jsonPath("$.netSavings").value(2600.00));
    }

    @Test
    @DisplayName("GET /api/reports/monthly returns 400 when month is invalid")
    void getMonthly_invalidMonth_returns400() throws Exception {
        when(reportService.getMonthlyReport(2026, 13))
                .thenThrow(new BusinessValidationException("month must be between 1 and 12"));

        mockMvc.perform(get("/api/reports/monthly/2026/13"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("GET /api/reports/monthly returns 400 when year is invalid")
    void getMonthly_invalidYear_returns400() throws Exception {
        when(reportService.getMonthlyReport(1800, 6))
                .thenThrow(new BusinessValidationException("year must be between 1900 and 2100"));

        mockMvc.perform(get("/api/reports/monthly/1800/6"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("GET /api/reports/monthly returns 400 when path variable is not an integer")
    void getMonthly_nonIntegerPath_returns400() throws Exception {
        mockMvc.perform(get("/api/reports/monthly/2026/abc"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("GET /api/reports/yearly/{year} returns 200 with report")
    void getYearly_validYear_returns200() throws Exception {
        var response = new YearlyReportResponse(2025,
                Map.of("Salary", new BigDecimal("36000.00")),
                Map.of("Rent", new BigDecimal("14400.00")),
                new BigDecimal("21600.00"));
        when(reportService.getYearlyReport(2025)).thenReturn(response);

        mockMvc.perform(get("/api/reports/yearly/2025"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.year").value(2025))
                .andExpect(jsonPath("$.netSavings").value(21600.00));
    }

    @Test
    @DisplayName("GET /api/reports/yearly returns 400 when year is invalid")
    void getYearly_invalidYear_returns400() throws Exception {
        when(reportService.getYearlyReport(1899))
                .thenThrow(new BusinessValidationException("year must be between 1900 and 2100"));

        mockMvc.perform(get("/api/reports/yearly/1899"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("GET /api/reports/yearly returns 400 when year is not an integer")
    void getYearly_nonIntegerYear_returns400() throws Exception {
        mockMvc.perform(get("/api/reports/yearly/notanumber"))
                .andExpect(status().isBadRequest());
    }
}
