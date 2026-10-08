package com.rubrangso.finance.category;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.rubrangso.finance.category.dto.CreateCategoryRequest;
import com.rubrangso.finance.common.exception.BusinessValidationException;
import com.rubrangso.finance.common.exception.DuplicateResourceException;
import com.rubrangso.finance.common.exception.ForbiddenOperationException;
import com.rubrangso.finance.common.exception.ResourceNotFoundException;
import com.rubrangso.finance.common.security.CurrentUserProvider;
import com.rubrangso.finance.transaction.TransactionRepository;
import com.rubrangso.finance.user.User;
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
class CategoryServiceTest {

    @Mock private CategoryRepository categoryRepository;
    @Mock private CurrentUserProvider currentUserProvider;
    @Mock private TransactionRepository transactionRepository;

    private CategoryService categoryService;
    private User user;

    @BeforeEach
    void setUp() {
        categoryService = new CategoryService(categoryRepository, currentUserProvider, transactionRepository);
        user = new User("Alice", "alice@example.com", "hashed", null);
        ReflectionTestUtils.setField(user, "id", 1L);
        when(currentUserProvider.getCurrentUser()).thenReturn(user);
    }

    @Test
    @DisplayName("getAllCategories returns defaults and user custom categories")
    void getAllCategories_returnsMergedList() {
        var salary = new Category("Salary", CategoryType.INCOME, false, null);
        ReflectionTestUtils.setField(salary, "id", 1L);
        var custom = new Category("Freelance", CategoryType.INCOME, true, user);
        ReflectionTestUtils.setField(custom, "id", 9L);

        when(categoryRepository.findByUserIsNull()).thenReturn(List.of(salary));
        when(categoryRepository.findByUser(user)).thenReturn(List.of(custom));

        var result = categoryService.getAllCategories();

        assertThat(result).hasSize(2);
        assertThat(result).extracting("name").containsExactly("Salary", "Freelance");
    }

    @Test
    @DisplayName("createCategory succeeds and returns CategoryResponse with isCustom=true")
    void createCategory_valid_returnsResponse() {
        var request = new CreateCategoryRequest("Freelance", CategoryType.INCOME);
        var saved = new Category("Freelance", CategoryType.INCOME, true, user);
        ReflectionTestUtils.setField(saved, "id", 9L);

        when(categoryRepository.existsByNameIgnoreCaseForUser("Freelance", user)).thenReturn(false);
        when(categoryRepository.save(any(Category.class))).thenReturn(saved);

        var response = categoryService.createCategory(request);

        assertThat(response.name()).isEqualTo("Freelance");
        assertThat(response.custom()).isTrue();
        assertThat(response.type()).isEqualTo(CategoryType.INCOME);
    }

    @Test
    @DisplayName("createCategory throws 409 when name clashes case-insensitively")
    void createCategory_duplicateName_throwsDuplicateResourceException() {
        var request = new CreateCategoryRequest("food", CategoryType.EXPENSE);
        when(categoryRepository.existsByNameIgnoreCaseForUser("food", user)).thenReturn(true);

        assertThatThrownBy(() -> categoryService.createCategory(request))
                .isInstanceOf(DuplicateResourceException.class);
    }

    @Test
    @DisplayName("deleteCategory removes a custom category")
    void deleteCategory_customCategory_deletesIt() {
        var category = new Category("Freelance", CategoryType.INCOME, true, user);
        ReflectionTestUtils.setField(category, "id", 9L);
        when(categoryRepository.findByNameIgnoreCaseForUser("Freelance", user))
                .thenReturn(Optional.of(category));

        categoryService.deleteCategory("Freelance");

        verify(categoryRepository).delete(category);
    }

    @Test
    @DisplayName("deleteCategory throws 403 when attempting to delete a system default")
    void deleteCategory_systemDefault_throwsForbiddenOperationException() {
        var category = new Category("Salary", CategoryType.INCOME, false, null);
        ReflectionTestUtils.setField(category, "id", 1L);
        when(categoryRepository.findByNameIgnoreCaseForUser("Salary", user))
                .thenReturn(Optional.of(category));

        assertThatThrownBy(() -> categoryService.deleteCategory("Salary"))
                .isInstanceOf(ForbiddenOperationException.class);
    }

    @Test
    @DisplayName("deleteCategory throws 400 when category is in use by transactions")
    void deleteCategory_inUse_throwsBusinessValidationException() {
        var category = new Category("Freelance", CategoryType.INCOME, true, user);
        ReflectionTestUtils.setField(category, "id", 9L);
        when(categoryRepository.findByNameIgnoreCaseForUser("Freelance", user))
                .thenReturn(Optional.of(category));
        when(transactionRepository.existsByCategoryId(9L)).thenReturn(true);

        assertThatThrownBy(() -> categoryService.deleteCategory("Freelance"))
                .isInstanceOf(BusinessValidationException.class);
    }

    @Test
    @DisplayName("deleteCategory throws 404 when category not found")
    void deleteCategory_unknownName_throwsResourceNotFoundException() {
        when(categoryRepository.findByNameIgnoreCaseForUser("Unknown", user))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> categoryService.deleteCategory("Unknown"))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
