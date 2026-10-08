package com.rubrangso.finance.goal;

import com.rubrangso.finance.goal.dto.CreateGoalRequest;
import com.rubrangso.finance.goal.dto.GoalResponse;
import com.rubrangso.finance.goal.dto.UpdateGoalRequest;
import jakarta.validation.Valid;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST endpoints for savings goal management.
 * All endpoints require authentication.
 */
@RestController
@RequestMapping("/api/goals")
public class GoalController {

    private final SavingsGoalService goalService;

    public GoalController(SavingsGoalService goalService) {
        this.goalService = goalService;
    }

    /** Returns all goals for the authenticated user. */
    @GetMapping
    public Map<String, Object> getAllGoals() {
        return Map.of("goals", goalService.getAllGoals());
    }

    /** Creates a new savings goal. */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public GoalResponse createGoal(@Valid @RequestBody CreateGoalRequest request) {
        return goalService.createGoal(request);
    }

    /** Returns a single goal by ID. */
    @GetMapping("/{id}")
    public GoalResponse getGoal(@PathVariable Long id) {
        return goalService.getGoal(id);
    }

    /** Updates targetAmount and/or targetDate of a goal. */
    @PutMapping("/{id}")
    public GoalResponse updateGoal(
            @PathVariable Long id,
            @Valid @RequestBody UpdateGoalRequest request) {
        return goalService.updateGoal(id, request);
    }

    /** Deletes a goal. */
    @DeleteMapping("/{id}")
    public Map<String, String> deleteGoal(@PathVariable Long id) {
        goalService.deleteGoal(id);
        return Map.of("message", "Goal deleted successfully");
    }
}
