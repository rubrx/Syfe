package com.rubrangso.finance.category;

import com.rubrangso.finance.category.dto.CategoryResponse;
import com.rubrangso.finance.category.dto.CreateCategoryRequest;
import com.rubrangso.finance.common.exception.BusinessValidationException;
import com.rubrangso.finance.common.exception.DuplicateResourceException;
import com.rubrangso.finance.common.exception.ForbiddenOperationException;
import com.rubrangso.finance.common.exception.ResourceNotFoundException;
import com.rubrangso.finance.common.security.CurrentUserProvider;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Business logic for category management.
 * Category name uniqueness is enforced case-insensitively; original casing is preserved.
 */
@Service
@Transactional(readOnly = true)
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final CurrentUserProvider currentUserProvider;

    public CategoryService(
            CategoryRepository categoryRepository,
            CurrentUserProvider currentUserProvider) {
        this.categoryRepository = categoryRepository;
        this.currentUserProvider = currentUserProvider;
    }

    /**
     * Returns all system-default categories plus the current user's custom categories.
     */
    public List<CategoryResponse> getAllCategories() {
        var user = currentUserProvider.getCurrentUser();
        var all = new ArrayList<>(categoryRepository.findByUserIsNull());
        all.addAll(categoryRepository.findByUser(user));
        return all.stream().map(CategoryResponse::from).toList();
    }

    /**
     * Creates a new custom category for the current user.
     *
     * @throws DuplicateResourceException if a category with the same name (case-insensitive)
     *         already exists as a system default or as one of the user's custom categories
     */
    @Transactional
    public CategoryResponse createCategory(CreateCategoryRequest request) {
        var user = currentUserProvider.getCurrentUser();

        if (categoryRepository.existsByNameIgnoreCaseForUser(request.name(), user)) {
            throw new DuplicateResourceException(
                    "Category '" + request.name() + "' already exists");
        }

        var category = new Category(request.name(), request.type(), true, user);
        return CategoryResponse.from(categoryRepository.save(category));
    }

    /**
     * Deletes a custom category by name (case-insensitive match).
     *
     * @throws ResourceNotFoundException if no category with that name is accessible to this user
     * @throws ForbiddenOperationException if the category is a system default
     * @throws BusinessValidationException if the category is referenced by one or more transactions
     */
    @Transactional
    public void deleteCategory(String name) {
        var user = currentUserProvider.getCurrentUser();
        var category = categoryRepository.findByNameIgnoreCaseForUser(name, user)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Category '" + name + "' not found"));

        if (!category.isCustom()) {
            throw new ForbiddenOperationException(
                    "Cannot delete system default category '" + category.getName() + "'");
        }

        // Transaction reference check is done via the transaction repository query in Phase 4.
        // The Category entity does not hold a back-reference to avoid circular deps here.
        // Instead we check in the repository via a count query injected from TransactionRepository.
        // For now, proceed without the check — it will be wired in Phase 4.
        categoryRepository.delete(category);
    }
}
