package com.rubrangso.finance.goal;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rubrangso.finance.common.exception.ForbiddenOperationException;
import com.rubrangso.finance.common.exception.GlobalExceptionHandler;
import com.rubrangso.finance.common.exception.ResourceNotFoundException;
import com.rubrangso.finance.config.SecurityConfig;
import com.rubrangso.finance.goal.dto.CreateGoalRequest;
import com.rubrangso.finance.goal.dto.GoalResponse;
import com.rubrangso.finance.goal.dto.UpdateGoalRequest;
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

@WebMvcTest(GoalController.class)
@Import({SecurityConfig.class, GlobalExceptionHandler.class})
@WithMockUser
class GoalControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    @MockitoBean SavingsGoalService goalService;
    @MockitoBean AppUserDetailsService appUserDetailsService;

    private static final LocalDate TODAY = LocalDate.of(2026, 6, 15);

    private GoalResponse sampleGoal() {
        return new GoalResponse(1L, "Emergency Fund", new BigDecimal("5000.00"),
                TODAY.plusMonths(6), TODAY, new BigDecimal("1000.00"),
                new BigDecimal("20.00"), new BigDecimal("4000.00"));
    }

    @Test
    @DisplayName("GET /api/goals returns 200 with goals list")
    void getAll_returns200() throws Exception {
        when(goalService.getAllGoals()).thenReturn(List.of(sampleGoal()));

        mockMvc.perform(get("/api/goals"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.goals").isArray())
                .andExpect(jsonPath("$.goals[0].goalName").value("Emergency Fund"))
                .andExpect(jsonPath("$.goals[0].targetAmount").value(5000.00));
    }

    @Test
    @DisplayName("POST /api/goals returns 201 with created goal")
    void create_valid_returns201() throws Exception {
        var request = new CreateGoalRequest("Emergency Fund", new BigDecimal("5000"), TODAY.plusMonths(6), null);
        when(goalService.createGoal(any())).thenReturn(sampleGoal());

        mockMvc.perform(post("/api/goals")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.goalName").value("Emergency Fund"))
                .andExpect(jsonPath("$.currentProgress").value(1000.00));
    }

    @Test
    @DisplayName("POST /api/goals returns 400 when targetAmount is negative")
    void create_negativeAmount_returns400() throws Exception {
        var request = new CreateGoalRequest("Fund", new BigDecimal("-1"), TODAY.plusMonths(6), null);

        mockMvc.perform(post("/api/goals")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors").isArray());
    }

    @Test
    @DisplayName("GET /api/goals/{id} returns 200 with goal")
    void getById_returns200() throws Exception {
        when(goalService.getGoal(1L)).thenReturn(sampleGoal());

        mockMvc.perform(get("/api/goals/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    @DisplayName("GET /api/goals/{id} returns 404 when not found")
    void getById_notFound_returns404() throws Exception {
        when(goalService.getGoal(99L)).thenThrow(new ResourceNotFoundException("Not found"));

        mockMvc.perform(get("/api/goals/99"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("GET /api/goals/{id} returns 403 when belongs to another user")
    void getById_forbidden_returns403() throws Exception {
        when(goalService.getGoal(5L)).thenThrow(new ForbiddenOperationException("Forbidden"));

        mockMvc.perform(get("/api/goals/5"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("PUT /api/goals/{id} returns 200 with updated goal")
    void update_valid_returns200() throws Exception {
        var request = new UpdateGoalRequest(new BigDecimal("8000"), null);
        when(goalService.updateGoal(eq(1L), any())).thenReturn(sampleGoal());

        mockMvc.perform(put("/api/goals/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("PUT /api/goals/{id} returns 403 when belongs to another user")
    void update_forbidden_returns403() throws Exception {
        when(goalService.updateGoal(eq(5L), any()))
                .thenThrow(new ForbiddenOperationException("Forbidden"));

        mockMvc.perform(put("/api/goals/5")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new UpdateGoalRequest(null, null))))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("DELETE /api/goals/{id} returns 200 on success")
    void delete_existing_returns200() throws Exception {
        doNothing().when(goalService).deleteGoal(1L);

        mockMvc.perform(delete("/api/goals/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Goal deleted successfully"));
    }

    @Test
    @DisplayName("DELETE /api/goals/{id} returns 404 when not found")
    void delete_notFound_returns404() throws Exception {
        doThrow(new ResourceNotFoundException("Not found")).when(goalService).deleteGoal(99L);

        mockMvc.perform(delete("/api/goals/99"))
                .andExpect(status().isNotFound());
    }
}
