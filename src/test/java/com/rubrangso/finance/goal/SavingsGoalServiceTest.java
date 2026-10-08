package com.rubrangso.finance.goal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.rubrangso.finance.category.CategoryType;
import com.rubrangso.finance.common.exception.BusinessValidationException;
import com.rubrangso.finance.common.exception.ForbiddenOperationException;
import com.rubrangso.finance.common.exception.ResourceNotFoundException;
import com.rubrangso.finance.common.security.CurrentUserProvider;
import com.rubrangso.finance.goal.dto.CreateGoalRequest;
import com.rubrangso.finance.goal.dto.UpdateGoalRequest;
import com.rubrangso.finance.transaction.TransactionRepository;
import com.rubrangso.finance.user.User;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class SavingsGoalServiceTest {

    @Mock private SavingsGoalRepository goalRepository;
    @Mock private TransactionRepository transactionRepository;
    @Mock private CurrentUserProvider currentUserProvider;

    private static final LocalDate TODAY = LocalDate.of(2026, 6, 15);
    private final Clock clock = Clock.fixed(Instant.parse("2026-06-15T00:00:00Z"), ZoneId.of("UTC"));

    private SavingsGoalService goalService;
    private User user;
    private User otherUser;

    @BeforeEach
    void setUp() {
        goalService = new SavingsGoalService(goalRepository, transactionRepository, currentUserProvider, clock);
        user = new User("Alice", "alice@example.com", "hashed", null);
        ReflectionTestUtils.setField(user, "id", 1L);
        otherUser = new User("Bob", "bob@example.com", "hashed", null);
        ReflectionTestUtils.setField(otherUser, "id", 2L);
        when(currentUserProvider.getCurrentUser()).thenReturn(user);
    }

    @Test
    @DisplayName("createGoal_validRequest_returnsResponse")
    void createGoal_validRequest_returnsResponse() {
        var targetDate = TODAY.plusMonths(6);
        var request = new CreateGoalRequest("Emergency Fund", new BigDecimal("5000"), targetDate, null);
        var saved = new SavingsGoal("Emergency Fund", new BigDecimal("5000.00"), targetDate, TODAY, user);
        ReflectionTestUtils.setField(saved, "id", 1L);

        when(goalRepository.save(any(SavingsGoal.class))).thenReturn(saved);
        when(transactionRepository.sumAmountByUserAndTypeFrom(user, CategoryType.INCOME, TODAY))
                .thenReturn(new BigDecimal("2000.00"));
        when(transactionRepository.sumAmountByUserAndTypeFrom(user, CategoryType.EXPENSE, TODAY))
                .thenReturn(new BigDecimal("500.00"));

        var response = goalService.createGoal(request);

        assertThat(response.goalName()).isEqualTo("Emergency Fund");
        assertThat(response.targetAmount()).isEqualByComparingTo("5000.00");
        assertThat(response.currentProgress()).isEqualByComparingTo("1500.00");
        assertThat(response.progressPercentage()).isEqualTo(30.0);
        assertThat(response.remainingAmount()).isEqualByComparingTo("3500.00");
    }

    @Test
    @DisplayName("createGoal_startDateDefaultsToToday")
    void createGoal_startDateDefaultsToToday() {
        var targetDate = TODAY.plusMonths(1);
        var request = new CreateGoalRequest("Test", new BigDecimal("1000"), targetDate, null);
        var saved = new SavingsGoal("Test", new BigDecimal("1000.00"), targetDate, TODAY, user);
        ReflectionTestUtils.setField(saved, "id", 1L);

        when(goalRepository.save(any(SavingsGoal.class))).thenReturn(saved);
        when(transactionRepository.sumAmountByUserAndTypeFrom(any(), any(), any()))
                .thenReturn(BigDecimal.ZERO);

        var response = goalService.createGoal(request);

        assertThat(response.startDate()).isEqualTo(TODAY);
    }

    @Test
    @DisplayName("createGoal_targetDateNotInFuture_throwsBusinessValidationException")
    void createGoal_targetDateNotInFuture_throwsBusinessValidationException() {
        var request = new CreateGoalRequest("Test", new BigDecimal("1000"), TODAY, null);

        assertThatThrownBy(() -> goalService.createGoal(request))
                .isInstanceOf(BusinessValidationException.class);
    }

    @Test
    @DisplayName("createGoal_startDateAfterTargetDate_throwsBusinessValidationException")
    void createGoal_startDateAfterTargetDate_throwsBusinessValidationException() {
        var targetDate = TODAY.plusDays(5);
        var startDate = TODAY.plusDays(10);
        var request = new CreateGoalRequest("Test", new BigDecimal("1000"), targetDate, startDate);

        assertThatThrownBy(() -> goalService.createGoal(request))
                .isInstanceOf(BusinessValidationException.class);
    }

    @Test
    @DisplayName("getGoal_existingOwned_returnsResponse")
    void getGoal_existingOwned_returnsResponse() {
        var goal = new SavingsGoal("Fund", new BigDecimal("1000.00"), TODAY.plusMonths(1), TODAY, user);
        ReflectionTestUtils.setField(goal, "id", 1L);

        when(goalRepository.findById(1L)).thenReturn(Optional.of(goal));
        when(transactionRepository.sumAmountByUserAndTypeFrom(any(), any(), any()))
                .thenReturn(BigDecimal.ZERO);

        var response = goalService.getGoal(1L);

        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.goalName()).isEqualTo("Fund");
    }

    @Test
    @DisplayName("getGoal_notFound_throwsResourceNotFoundException")
    void getGoal_notFound_throwsResourceNotFoundException() {
        when(goalRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> goalService.getGoal(99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("getGoal_belongsToOtherUser_throwsForbiddenOperationException")
    void getGoal_belongsToOtherUser_throwsForbiddenOperationException() {
        var goal = new SavingsGoal("Fund", new BigDecimal("1000.00"), TODAY.plusMonths(1), TODAY, otherUser);
        ReflectionTestUtils.setField(goal, "id", 5L);
        when(goalRepository.findById(5L)).thenReturn(Optional.of(goal));

        assertThatThrownBy(() -> goalService.getGoal(5L))
                .isInstanceOf(ForbiddenOperationException.class);
    }

    @Test
    @DisplayName("getAllGoals_delegatesToRepository")
    void getAllGoals_delegatesToRepository() {
        when(goalRepository.findByUser(user)).thenReturn(List.of());

        var result = goalService.getAllGoals();

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("updateGoal_newTargetAmount_appliesChange")
    void updateGoal_newTargetAmount_appliesChange() {
        var goal = new SavingsGoal("Fund", new BigDecimal("1000.00"), TODAY.plusMonths(3), TODAY, user);
        ReflectionTestUtils.setField(goal, "id", 1L);
        var request = new UpdateGoalRequest(new BigDecimal("2000"), null);

        when(goalRepository.findById(1L)).thenReturn(Optional.of(goal));
        when(goalRepository.save(goal)).thenReturn(goal);
        when(transactionRepository.sumAmountByUserAndTypeFrom(any(), any(), any()))
                .thenReturn(BigDecimal.ZERO);

        var response = goalService.updateGoal(1L, request);

        assertThat(response.targetAmount()).isEqualByComparingTo("2000.00");
    }

    @Test
    @DisplayName("updateGoal_newTargetDateInPast_throwsBusinessValidationException")
    void updateGoal_newTargetDateInPast_throwsBusinessValidationException() {
        var goal = new SavingsGoal("Fund", new BigDecimal("1000.00"), TODAY.plusMonths(3), TODAY, user);
        ReflectionTestUtils.setField(goal, "id", 1L);
        var request = new UpdateGoalRequest(null, TODAY.minusDays(1));

        when(goalRepository.findById(1L)).thenReturn(Optional.of(goal));

        assertThatThrownBy(() -> goalService.updateGoal(1L, request))
                .isInstanceOf(BusinessValidationException.class);
    }

    @Test
    @DisplayName("deleteGoal_existing_deletesIt")
    void deleteGoal_existing_deletesIt() {
        var goal = new SavingsGoal("Fund", new BigDecimal("1000.00"), TODAY.plusMonths(1), TODAY, user);
        ReflectionTestUtils.setField(goal, "id", 1L);
        when(goalRepository.findById(1L)).thenReturn(Optional.of(goal));

        goalService.deleteGoal(1L);

        verify(goalRepository).delete(goal);
    }

    @Test
    @DisplayName("deleteGoal_belongsToOtherUser_throwsForbiddenOperationException")
    void deleteGoal_belongsToOtherUser_throwsForbiddenOperationException() {
        var goal = new SavingsGoal("Fund", new BigDecimal("1000.00"), TODAY.plusMonths(1), TODAY, otherUser);
        ReflectionTestUtils.setField(goal, "id", 5L);
        when(goalRepository.findById(5L)).thenReturn(Optional.of(goal));

        assertThatThrownBy(() -> goalService.deleteGoal(5L))
                .isInstanceOf(ForbiddenOperationException.class);
    }

    @Test
    @DisplayName("progressComputation_negativeProgressIsValid")
    void progressComputation_negativeProgressIsValid() {
        var targetDate = TODAY.plusMonths(6);
        var request = new CreateGoalRequest("Fund", new BigDecimal("5000"), targetDate, null);
        var saved = new SavingsGoal("Fund", new BigDecimal("5000.00"), targetDate, TODAY, user);
        ReflectionTestUtils.setField(saved, "id", 1L);

        when(goalRepository.save(any(SavingsGoal.class))).thenReturn(saved);
        when(transactionRepository.sumAmountByUserAndTypeFrom(user, CategoryType.INCOME, TODAY))
                .thenReturn(new BigDecimal("100.00"));
        when(transactionRepository.sumAmountByUserAndTypeFrom(user, CategoryType.EXPENSE, TODAY))
                .thenReturn(new BigDecimal("500.00"));

        var response = goalService.createGoal(request);

        assertThat(response.currentProgress()).isEqualByComparingTo("-400.00");
        assertThat(response.progressPercentage()).isNegative();
        assertThat(response.remainingAmount()).isGreaterThan(saved.getTargetAmount());
    }
}
