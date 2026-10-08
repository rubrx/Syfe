package com.rubrangso.finance.goal.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import java.time.LocalDate;

/** Request body for POST /api/goals. */
public record CreateGoalRequest(
        @NotBlank String goalName,
        @NotNull @Positive BigDecimal targetAmount,
        @NotNull LocalDate targetDate,
        LocalDate startDate) {}
