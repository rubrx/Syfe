package com.rubrangso.finance.category.dto;

import com.rubrangso.finance.category.CategoryType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateCategoryRequest(

        @NotBlank(message = "Category name is required")
        String name,

        @NotNull(message = "Category type is required")
        CategoryType type
) {}
