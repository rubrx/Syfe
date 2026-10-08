package com.rubrangso.finance.category;

import com.rubrangso.finance.category.dto.CategoryResponse;
import com.rubrangso.finance.category.dto.CreateCategoryRequest;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** CRUD endpoints for transaction categories. */
@RestController
@RequestMapping("/api/categories")
public class CategoryController {

    private final CategoryService categoryService;

    public CategoryController(CategoryService categoryService) {
        this.categoryService = categoryService;
    }

    /**
     * Lists all categories accessible to the current user (system defaults + custom).
     */
    @GetMapping
    public Map<String, List<CategoryResponse>> getAll() {
        return Map.of("categories", categoryService.getAllCategories());
    }

    /**
     * Creates a new custom category for the current user.
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CategoryResponse create(@Valid @RequestBody CreateCategoryRequest request) {
        return categoryService.createCategory(request);
    }

    /**
     * Deletes a custom category by name. URL-encoded names are decoded by Spring MVC.
     */
    @DeleteMapping("/{name}")
    public Map<String, String> delete(@PathVariable String name) {
        categoryService.deleteCategory(name);
        return Map.of("message", "Category deleted successfully");
    }
}
