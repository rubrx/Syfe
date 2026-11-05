package com.rubrangso.finance.transaction;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rubrangso.finance.category.CategoryType;
import com.rubrangso.finance.common.exception.BusinessValidationException;
import com.rubrangso.finance.common.exception.GlobalExceptionHandler;
import com.rubrangso.finance.common.exception.ResourceNotFoundException;
import com.rubrangso.finance.config.SecurityConfig;
import com.rubrangso.finance.transaction.dto.CreateTransactionRequest;
import com.rubrangso.finance.transaction.dto.TransactionResponse;
import com.rubrangso.finance.transaction.dto.UpdateTransactionRequest;
import com.rubrangso.finance.user.AppUserDetailsService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(TransactionController.class)
@Import({SecurityConfig.class, GlobalExceptionHandler.class})
@WithMockUser
class TransactionControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    @MockitoBean TransactionService transactionService;
    @MockitoBean AppUserDetailsService appUserDetailsService;

    private static final LocalDate TODAY = LocalDate.of(2026, 6, 15);

    @Test
    @DisplayName("GET /api/transactions returns 200 with transactions list")
    void getAll_returns200() throws Exception {
        var tx = new TransactionResponse(1L, new BigDecimal("5000.00"), TODAY, "Salary", "Jan", CategoryType.INCOME);
        when(transactionService.getTransactions(any(), any(), any(), any())).thenReturn(List.of(tx));

        mockMvc.perform(get("/api/transactions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.transactions").isArray())
                .andExpect(jsonPath("$.transactions[0].category").value("Salary"))
                .andExpect(jsonPath("$.transactions[0].type").value("INCOME"));
    }

    @Test
    @DisplayName("POST /api/transactions returns 201 with created transaction")
    void create_valid_returns201() throws Exception {
        var request = new CreateTransactionRequest(new BigDecimal("5000"), TODAY, "Salary", null);
        var response = new TransactionResponse(1L, new BigDecimal("5000.00"), TODAY, "Salary", null, CategoryType.INCOME);
        when(transactionService.createTransaction(any())).thenReturn(response);

        mockMvc.perform(post("/api/transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.amount").value(5000.00))
                .andExpect(jsonPath("$.type").value("INCOME"));
    }

    @Test
    @DisplayName("POST /api/transactions returns 400 when amount is negative")
    void create_negativeAmount_returns400() throws Exception {
        var request = new CreateTransactionRequest(new BigDecimal("-1"), TODAY, "Salary", null);

        mockMvc.perform(post("/api/transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors").isArray());
    }

    @Test
    @DisplayName("PUT /api/transactions/{id} returns 200 with updated transaction")
    void update_valid_returns200() throws Exception {
        var request = new UpdateTransactionRequest(new BigDecimal("6000"), null, "updated");
        var response = new TransactionResponse(1L, new BigDecimal("6000.00"), TODAY, "Salary", "updated", CategoryType.INCOME);
        when(transactionService.updateTransaction(eq(1L), any())).thenReturn(response);

        mockMvc.perform(put("/api/transactions/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.amount").value(6000.00));
    }

    @Test
    @DisplayName("PUT /api/transactions/{id} returns 404 when not found")
    void update_notFound_returns404() throws Exception {
        when(transactionService.updateTransaction(eq(99L), any()))
                .thenThrow(new ResourceNotFoundException("Not found"));

        mockMvc.perform(put("/api/transactions/99")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new UpdateTransactionRequest(null, null, null))))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("DELETE /api/transactions/{id} returns 200 on success")
    void delete_existing_returns200() throws Exception {
        doNothing().when(transactionService).deleteTransaction(1L);

        mockMvc.perform(delete("/api/transactions/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Transaction deleted successfully"));
    }

    @Test
    @DisplayName("DELETE /api/transactions/{id} returns 404 when not found")
    void delete_notFound_returns404() throws Exception {
        doThrow(new ResourceNotFoundException("Not found"))
                .when(transactionService).deleteTransaction(99L);

        mockMvc.perform(delete("/api/transactions/99"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("GET /api/transactions/top-expenses returns 200 with expenses array")
    void getTopExpenses_valid_returns200WithExpensesArray() throws Exception {
        var tx = new TransactionResponse(1L, new BigDecimal("500.00"), TODAY, "Food", "lunch", CategoryType.EXPENSE);
        when(transactionService.getTopExpenses(2026, 6, 3)).thenReturn(List.of(tx));

        mockMvc.perform(get("/api/transactions/top-expenses")
                        .param("year", "2026")
                        .param("month", "6")
                        .param("k", "3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.expenses").isArray())
                .andExpect(jsonPath("$.expenses[0].amount").value(500.00))
                .andExpect(jsonPath("$.expenses[0].type").value("EXPENSE"));
    }

    @Test
    @DisplayName("GET /api/transactions/top-expenses defaults k to 5 when omitted")
    void getTopExpenses_missingK_defaultsTo5() throws Exception {
        when(transactionService.getTopExpenses(2026, 6, 5)).thenReturn(List.of());

        mockMvc.perform(get("/api/transactions/top-expenses")
                        .param("year", "2026")
                        .param("month", "6"))
                .andExpect(status().isOk());

        verify(transactionService).getTopExpenses(2026, 6, 5);
    }

    @Test
    @DisplayName("GET /api/transactions/top-expenses returns 200 with empty list when no expenses")
    void getTopExpenses_noExpenses_returns200WithEmptyList() throws Exception {
        when(transactionService.getTopExpenses(2026, 6, 5)).thenReturn(List.of());

        mockMvc.perform(get("/api/transactions/top-expenses")
                        .param("year", "2026")
                        .param("month", "6"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.expenses").isArray())
                .andExpect(jsonPath("$.expenses").isEmpty());
    }

    @Test
    @DisplayName("GET /api/transactions/top-expenses returns 400 when month is invalid")
    void getTopExpenses_invalidMonth_returns400() throws Exception {
        when(transactionService.getTopExpenses(2026, 13, 5))
                .thenThrow(new BusinessValidationException("month must be between 1 and 12"));

        mockMvc.perform(get("/api/transactions/top-expenses")
                        .param("year", "2026")
                        .param("month", "13"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("month must be between 1 and 12"));
    }

    @Test
    @DisplayName("GET /api/transactions/top-expenses returns 400 when k is less than 1")
    void getTopExpenses_kLessThanOne_returns400() throws Exception {
        when(transactionService.getTopExpenses(2026, 6, 0))
                .thenThrow(new BusinessValidationException("k must be at least 1"));

        mockMvc.perform(get("/api/transactions/top-expenses")
                        .param("year", "2026")
                        .param("month", "6")
                        .param("k", "0"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("k must be at least 1"));
    }

    @Test
    @DisplayName("GET /api/transactions/top-expenses returns 400 when year is missing")
    void getTopExpenses_missingYear_returns400() throws Exception {
        mockMvc.perform(get("/api/transactions/top-expenses")
                        .param("month", "6"))
                .andExpect(status().isBadRequest());
    }
}
