package com.rubrangso.finance.auth;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rubrangso.finance.auth.dto.AuthResponse;
import com.rubrangso.finance.auth.dto.LoginRequest;
import com.rubrangso.finance.auth.dto.RegisterRequest;
import com.rubrangso.finance.common.exception.DuplicateResourceException;
import com.rubrangso.finance.common.exception.GlobalExceptionHandler;
import com.rubrangso.finance.config.SecurityConfig;
import com.rubrangso.finance.user.AppUserDetailsService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(AuthController.class)
@Import({SecurityConfig.class, GlobalExceptionHandler.class})
class AuthControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    @MockitoBean AuthService authService;
    @MockitoBean AppUserDetailsService appUserDetailsService;

    @Test
    @DisplayName("POST /api/auth/register returns 201 with user body")
    void register_valid_returns201() throws Exception {
        var request = new RegisterRequest("Alice", "alice@example.com", "password1", null);
        var response = new AuthResponse(1L, "Alice", "alice@example.com", null);
        when(authService.register(any())).thenReturn(response);

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value("alice@example.com"))
                .andExpect(jsonPath("$.name").value("Alice"))
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    @DisplayName("POST /api/auth/register returns 400 when name is blank")
    void register_blankName_returns400() throws Exception {
        var request = new RegisterRequest("", "alice@example.com", "password1", null);

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors").isArray());
    }

    @Test
    @DisplayName("POST /api/auth/register returns 400 when password is too short")
    void register_shortPassword_returns400() throws Exception {
        var request = new RegisterRequest("Alice", "alice@example.com", "abc", null);

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /api/auth/register returns 409 on duplicate email")
    void register_duplicateEmail_returns409() throws Exception {
        var request = new RegisterRequest("Alice", "alice@example.com", "password1", null);
        when(authService.register(any())).thenThrow(new DuplicateResourceException("Email already registered"));

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("POST /api/auth/login returns 200 with user body")
    void login_validCredentials_returns200() throws Exception {
        var request = new LoginRequest("alice@example.com", "password1");
        var response = new AuthResponse(1L, "Alice", "alice@example.com", null);
        when(authService.login(any(), any(), any())).thenReturn(response);

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("alice@example.com"));
    }

    @Test
    @DisplayName("POST /api/auth/login returns 401 on bad credentials")
    void login_badCredentials_returns401() throws Exception {
        var request = new LoginRequest("alice@example.com", "wrong");
        when(authService.login(any(), any(), any()))
                .thenThrow(new BadCredentialsException("Bad credentials"));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("POST /api/auth/logout returns 200 when authenticated")
    @WithMockUser
    void logout_authenticated_returns200() throws Exception {
        doNothing().when(authService).logout(any());

        mockMvc.perform(post("/api/auth/logout"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Logged out successfully"));
    }

    @Test
    @DisplayName("POST /api/auth/logout returns 401 when not authenticated")
    void logout_unauthenticated_returns401() throws Exception {
        mockMvc.perform(post("/api/auth/logout"))
                .andExpect(status().isUnauthorized());
    }
}
