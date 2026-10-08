package com.rubrangso.finance.goal;

import com.rubrangso.finance.category.CategoryType;
import com.rubrangso.finance.common.exception.BusinessValidationException;
import com.rubrangso.finance.common.exception.ForbiddenOperationException;
import com.rubrangso.finance.common.exception.ResourceNotFoundException;
import com.rubrangso.finance.common.security.CurrentUserProvider;
import com.rubrangso.finance.goal.dto.CreateGoalRequest;
import com.rubrangso.finance.goal.dto.GoalResponse;
import com.rubrangso.finance.goal.dto.UpdateGoalRequest;
import com.rubrangso.finance.transaction.TransactionRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Business logic for savings goals.
 * Progress is always computed live; no caching.
 */
@Service
@Transactional(readOnly = true)
public class SavingsGoalService {

    private final SavingsGoalRepository goalRepository;
    private final TransactionRepository transactionRepository;
    private final CurrentUserProvider currentUserProvider;
    private final Clock clock;

    public SavingsGoalService(
            SavingsGoalRepository goalRepository,
            TransactionRepository transactionRepository,
            CurrentUserProvider currentUserProvider,
            Clock clock) {
        this.goalRepository = goalRepository;
        this.transactionRepository = transactionRepository;
        this.currentUserProvider = currentUserProvider;
        this.clock = clock;
    }

    /** Returns all goals for the current user with live progress. */
    public List<GoalResponse> getAllGoals() {
        var user = currentUserProvider.getCurrentUser();
        return goalRepository.findByUser(user).stream()
                .map(this::toResponse)
                .toList();
    }

    /**
     * Returns a single goal with live progress.
     *
     * @throws ResourceNotFoundException if no goal with that ID exists
     * @throws ForbiddenOperationException if the goal belongs to another user
     */
    public GoalResponse getGoal(Long id) {
        var user = currentUserProvider.getCurrentUser();
        var goal = goalRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Goal not found: id=" + id));
        if (!goal.getUser().getId().equals(user.getId())) {
            throw new ForbiddenOperationException("Goal does not belong to the current user");
        }
        return toResponse(goal);
    }

    /**
     * Creates a new savings goal for the current user.
     *
     * @throws BusinessValidationException if targetDate is not in the future,
     *         or startDate is not before targetDate
     */
    @Transactional
    public GoalResponse createGoal(CreateGoalRequest request) {
        var user = currentUserProvider.getCurrentUser();
        var today = LocalDate.now(clock);

        if (!request.targetDate().isAfter(today)) {
            throw new BusinessValidationException("targetDate must be strictly in the future");
        }

        var startDate = request.startDate() != null ? request.startDate() : today;

        if (!startDate.isBefore(request.targetDate())) {
            throw new BusinessValidationException("startDate must be before targetDate");
        }

        var amount = request.targetAmount().setScale(2, RoundingMode.HALF_UP);
        var goal = new SavingsGoal(request.goalName(), amount, request.targetDate(), startDate, user);
        return toResponse(goalRepository.save(goal));
    }

    /**
     * Updates targetAmount and/or targetDate of an existing goal.
     *
     * @throws ResourceNotFoundException if goal ID does not exist
     * @throws ForbiddenOperationException if goal belongs to another user
     * @throws BusinessValidationException if new targetDate is not in the future or not after startDate
     */
    @Transactional
    public GoalResponse updateGoal(Long id, UpdateGoalRequest request) {
        var user = currentUserProvider.getCurrentUser();
        var today = LocalDate.now(clock);

        var goal = goalRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Goal not found: id=" + id));
        if (!goal.getUser().getId().equals(user.getId())) {
            throw new ForbiddenOperationException("Goal does not belong to the current user");
        }

        if (request.targetAmount() != null) {
            goal.setTargetAmount(request.targetAmount().setScale(2, RoundingMode.HALF_UP));
        }

        if (request.targetDate() != null) {
            if (!request.targetDate().isAfter(today)) {
                throw new BusinessValidationException("targetDate must be strictly in the future");
            }
            if (!request.targetDate().isAfter(goal.getStartDate())) {
                throw new BusinessValidationException("targetDate must be after startDate");
            }
            goal.setTargetDate(request.targetDate());
        }

        return toResponse(goalRepository.save(goal));
    }

    /**
     * Deletes a goal.
     *
     * @throws ResourceNotFoundException if goal ID does not exist
     * @throws ForbiddenOperationException if goal belongs to another user
     */
    @Transactional
    public void deleteGoal(Long id) {
        var user = currentUserProvider.getCurrentUser();
        var goal = goalRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Goal not found: id=" + id));
        if (!goal.getUser().getId().equals(user.getId())) {
            throw new ForbiddenOperationException("Goal does not belong to the current user");
        }
        goalRepository.delete(goal);
    }

    private GoalResponse toResponse(SavingsGoal goal) {
        var user = goal.getUser();
        var income = transactionRepository.sumAmountByUserAndTypeFrom(
                user, CategoryType.INCOME, goal.getStartDate());
        var expense = transactionRepository.sumAmountByUserAndTypeFrom(
                user, CategoryType.EXPENSE, goal.getStartDate());

        // Normalize zero so JSON serialises as 0, not 0.00
        var rawProgress = income.subtract(expense);
        var progress = rawProgress.signum() == 0
                ? BigDecimal.ZERO
                : rawProgress.setScale(2, RoundingMode.HALF_UP);
        var target = goal.getTargetAmount();

        // Multiply before dividing to preserve decimal precision (e.g. 65.5, not 66.00)
        var percentage = progress
                .multiply(BigDecimal.valueOf(100))
                .divide(target, 2, RoundingMode.HALF_UP)
                .doubleValue();
        var remaining = target.subtract(progress).setScale(2, RoundingMode.HALF_UP);

        return new GoalResponse(
                goal.getId(),
                goal.getGoalName(),
                target,
                goal.getTargetDate(),
                goal.getStartDate(),
                progress,
                percentage,
                remaining);
    }
}
