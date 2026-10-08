package com.rubrangso.finance.goal.dto;

import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import java.time.LocalDate;

/** Request body for PUT /api/goals/{id}. Only targetAmount and targetDate may be updated. */
public record UpdateGoalRequest(
        @Positive BigDecimal targetAmount,
        LocalDate targetDate) {}
