package com.rubrangso.finance.category;

import com.rubrangso.finance.user.User;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** Data access for {@link Category} records. */
public interface CategoryRepository extends JpaRepository<Category, Long> {

    /** All system defaults (user is null). */
    List<Category> findByUserIsNull();

    /** All custom categories for a specific user. */
    List<Category> findByUser(User user);

    /**
     * Case-insensitive name lookup across defaults and the given user's custom categories.
     * Used when creating transactions (category is referenced by name).
     */
    @Query("""
            SELECT c FROM Category c
            WHERE UPPER(c.name) = UPPER(:name)
              AND (c.user IS NULL OR c.user = :user)
            """)
    Optional<Category> findByNameIgnoreCaseForUser(@Param("name") String name, @Param("user") User user);

    /**
     * Checks whether a category with the given name already exists (case-insensitive) for
     * this user or as a system default. Used to enforce uniqueness on create.
     */
    @Query("""
            SELECT COUNT(c) > 0 FROM Category c
            WHERE UPPER(c.name) = UPPER(:name)
              AND (c.user IS NULL OR c.user = :user)
            """)
    boolean existsByNameIgnoreCaseForUser(@Param("name") String name, @Param("user") User user);
}
