package com.rubrangso.finance.transaction;

import com.rubrangso.finance.category.CategoryType;
import com.rubrangso.finance.user.User;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** Data access for {@link Transaction} records. All queries are scoped to the owning user. */
public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    /**
     * Returns transactions for the given user matching all non-null filters.
     * JOIN FETCH on category avoids N+1 when building response DTOs.
     */
    @Query("""
            SELECT t FROM Transaction t
            JOIN FETCH t.category c
            WHERE t.user = :user
              AND (:startDate IS NULL OR t.date >= :startDate)
              AND (:endDate IS NULL OR t.date <= :endDate)
              AND (:categoryName IS NULL OR LOWER(c.name) = LOWER(:categoryName))
              AND (:type IS NULL OR t.type = :type)
            ORDER BY t.date DESC, t.id DESC
            """)
    List<Transaction> findWithFilters(
            @Param("user") User user,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("categoryName") String categoryName,
            @Param("type") CategoryType type);

    /**
     * Owner-scoped lookup used for GET, PUT, DELETE. Another user's transaction returns empty
     * (treated as 404 per Ambiguity #4 in the API contract — no 403 for transactions).
     */
    Optional<Transaction> findByIdAndUser(Long id, User user);

    /** Used by CategoryService to block deletion of a category that is still in use. */
    boolean existsByCategoryId(Long categoryId);

    /**
     * Sums transaction amounts for the given user, type, and date range.
     * Used by SavingsGoalService to compute goal progress. Returns 0 when no rows match.
     */
    @Query("""
            SELECT COALESCE(SUM(t.amount), 0)
            FROM Transaction t
            WHERE t.user = :user
              AND t.type = :type
              AND t.date >= :startDate
            """)
    java.math.BigDecimal sumAmountByUserAndTypeFrom(
            @Param("user") com.rubrangso.finance.user.User user,
            @Param("type") com.rubrangso.finance.category.CategoryType type,
            @Param("startDate") java.time.LocalDate startDate);

    /**
     * Returns per-category sums for a given user, type, and date range.
     * Each element is [categoryName (String), total (BigDecimal)].
     * Used by ReportService. Only categories with at least one transaction are returned.
     */
    @Query("""
            SELECT t.category.name, SUM(t.amount)
            FROM Transaction t
            WHERE t.user = :user
              AND t.type = :type
              AND t.date >= :startDate
              AND t.date <= :endDate
            GROUP BY t.category.name
            """)
    java.util.List<Object[]> sumByCategoryForUserAndType(
            @Param("user") com.rubrangso.finance.user.User user,
            @Param("type") com.rubrangso.finance.category.CategoryType type,
            @Param("startDate") java.time.LocalDate startDate,
            @Param("endDate") java.time.LocalDate endDate);
}
