package com.rubrangso.finance.goal;

import com.rubrangso.finance.user.User;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/** Data access for {@link SavingsGoal} records. */
public interface SavingsGoalRepository extends JpaRepository<SavingsGoal, Long> {

    /** All goals owned by the given user. */
    List<SavingsGoal> findByUser(User user);

    /**
     * Two-step ownership lookup: first call {@link #findById} to distinguish 404 vs 403,
     * then check {@link SavingsGoal#getUser()} to enforce ownership.
     */
    Optional<SavingsGoal> findByIdAndUser(Long id, User user);
}
