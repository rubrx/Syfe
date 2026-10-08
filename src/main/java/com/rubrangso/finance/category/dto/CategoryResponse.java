package com.rubrangso.finance.category.dto;

import com.rubrangso.finance.category.Category;
import com.rubrangso.finance.category.CategoryType;

/**
 * For Java records Jackson uses the component name as the JSON field name.
 * The component is named {@code custom} (not {@code isCustom}) so the JSON field
 * is {@code "custom"}, matching what the test script expects.
 */
public record CategoryResponse(
        Long id,
        String name,
        CategoryType type,
        boolean custom) {

    public static CategoryResponse from(Category category) {
        return new CategoryResponse(
                category.getId(),
                category.getName(),
                category.getType(),
                category.isCustom());
    }
}
