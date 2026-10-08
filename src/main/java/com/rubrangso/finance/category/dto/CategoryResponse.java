package com.rubrangso.finance.category.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.rubrangso.finance.category.Category;
import com.rubrangso.finance.category.CategoryType;

/**
 * {@code isCustom} is serialised with {@code @JsonProperty} because Jackson strips the {@code is}
 * prefix from boolean accessors by default, which would produce {@code "custom"} instead.
 */
public record CategoryResponse(
        Long id,
        String name,
        CategoryType type,
        @JsonProperty("isCustom") boolean isCustom) {

    public static CategoryResponse from(Category category) {
        return new CategoryResponse(
                category.getId(),
                category.getName(),
                category.getType(),
                category.isCustom());
    }
}
