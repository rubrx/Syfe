package com.rubrangso.finance.category;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rubrangso.finance.category.dto.CategoryResponse;
import com.rubrangso.finance.category.dto.CreateCategoryRequest;
import com.rubrangso.finance.common.exception.DuplicateResourceException;
import com.rubrangso.finance.common.exception.ForbiddenOperationException;
import com.rubrangso.finance.common.exception.GlobalExceptionHandler;
import com.rubrangso.finance.common.exception.ResourceNotFoundException;
import com.rubrangso.finance.config.SecurityConfig;
import com.rubrangso.finance.user.AppUserDetailsService;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(CategoryController.class)
@Import({SecurityConfig.class, GlobalExceptionHandler.class})
@WithMockUser
class CategoryControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    @MockitoBean CategoryService categoryService;
    @MockitoBean AppUserDetailsService appUserDetailsService;

    @Test
    @DisplayName("GET /api/categories returns 200 with categories list")
    void getAll_returns200WithList() throws Exception {
        var categories = List.of(
                new CategoryResponse(1L, "Salary", CategoryType.INCOME, false),
                new CategoryResponse(9L, "Freelance", CategoryType.INCOME, true));
        when(categoryService.getAllCategories()).thenReturn(categories);

        mockMvc.perform(get("/api/categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.categories").isArray())
                .andExpect(jsonPath("$.categories.length()").value(2))
                .andExpect(jsonPath("$.categories[0].isCustom").value(false))
                .andExpect(jsonPath("$.categories[1].isCustom").value(true));
    }

    @Test
    @DisplayName("POST /api/categories returns 201 with created category")
    void create_valid_returns201() throws Exception {
        var request = new CreateCategoryRequest("Freelance", CategoryType.INCOME);
        var response = new CategoryResponse(9L, "Freelance", CategoryType.INCOME, true);
        when(categoryService.createCategory(any())).thenReturn(response);

        mockMvc.perform(post("/api/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Freelance"))
                .andExpect(jsonPath("$.isCustom").value(true));
    }

    @Test
    @DisplayName("POST /api/categories returns 400 when name is blank")
    void create_blankName_returns400() throws Exception {
        var request = new CreateCategoryRequest("", CategoryType.INCOME);

        mockMvc.perform(post("/api/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors").isArray());
    }

    @Test
    @DisplayName("POST /api/categories returns 409 on duplicate name")
    void create_duplicateName_returns409() throws Exception {
        var request = new CreateCategoryRequest("Food", CategoryType.EXPENSE);
        when(categoryService.createCategory(any())).thenThrow(new DuplicateResourceException("Already exists"));

        mockMvc.perform(post("/api/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("DELETE /api/categories/{name} returns 200 on success")
    void delete_customCategory_returns200() throws Exception {
        doNothing().when(categoryService).deleteCategory("Freelance");

        mockMvc.perform(delete("/api/categories/Freelance"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Category deleted successfully"));
    }

    @Test
    @DisplayName("DELETE /api/categories/{name} returns 403 for system default")
    void delete_systemDefault_returns403() throws Exception {
        doThrow(new ForbiddenOperationException("Cannot delete system default"))
                .when(categoryService).deleteCategory(eq("Salary"));

        mockMvc.perform(delete("/api/categories/Salary"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("DELETE /api/categories/{name} returns 404 for unknown name")
    void delete_unknownName_returns404() throws Exception {
        doThrow(new ResourceNotFoundException("Not found"))
                .when(categoryService).deleteCategory(eq("Unknown"));

        mockMvc.perform(delete("/api/categories/Unknown"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("GET /api/categories returns 401 when not authenticated")
    @WithAnonymousUser
    void getAll_unauthenticated_returns401() throws Exception {
        mockMvc.perform(get("/api/categories"))
                .andExpect(status().isUnauthorized());
    }
}
