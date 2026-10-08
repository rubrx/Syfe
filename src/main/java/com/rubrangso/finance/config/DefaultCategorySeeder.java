package com.rubrangso.finance.config;

import com.rubrangso.finance.category.Category;
import com.rubrangso.finance.category.CategoryRepository;
import com.rubrangso.finance.category.CategoryType;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Seeds the seven system-default categories on startup if they are not already present.
 * These rows have {@code user = null} and {@code custom = false}.
 * Running at startup (not via a schema script) keeps the seed logic in Java where it is
 * easy to test and guarantees idempotency across {@code create-drop} restarts.
 */
@Component
public class DefaultCategorySeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DefaultCategorySeeder.class);

    private static final List<Object[]> DEFAULTS = List.of(
            new Object[]{"Salary",          CategoryType.INCOME},
            new Object[]{"Food",            CategoryType.EXPENSE},
            new Object[]{"Rent",            CategoryType.EXPENSE},
            new Object[]{"Transportation",  CategoryType.EXPENSE},
            new Object[]{"Entertainment",   CategoryType.EXPENSE},
            new Object[]{"Healthcare",      CategoryType.EXPENSE},
            new Object[]{"Utilities",       CategoryType.EXPENSE}
    );

    private final CategoryRepository categoryRepository;

    public DefaultCategorySeeder(CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        var existing = categoryRepository.findByUserIsNull();
        if (!existing.isEmpty()) {
            log.debug("Default categories already seeded ({} found); skipping.", existing.size());
            return;
        }
        for (var row : DEFAULTS) {
            categoryRepository.save(new Category((String) row[0], (CategoryType) row[1], false, null));
        }
        log.info("Seeded {} default categories.", DEFAULTS.size());
    }
}
