package com.rubrangso.finance.goal.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Read model returned for every goal endpoint. Progress fields are always live-computed. */
public record GoalResponse(
        Long id,
        String goalName,
        BigDecimal targetAmount,
        LocalDate targetDate,
        LocalDate startDate,
        BigDecimal currentProgress,
        Double progressPercentage,
        BigDecimal remainingAmount) {}
