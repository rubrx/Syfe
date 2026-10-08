package com.rubrangso.finance.auth;

import static org.mockito.ArgumentMatchers.any;
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
    @DisplayName("POST /api/auth/register returns 201 with message and userId")
    void register_valid_returns201() throws Exception {
        var request = new RegisterRequest("Alice", "alice@example.com", "password1a", null);
        when(authService.register(any())).thenReturn(new AuthResponse("User registered successfully", 1L));

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.message").value("User registered successfully"))
                .andExpect(jsonPath("$.userId").value(1));
    }

    @Test
    @DisplayName("POST /api/auth/register returns 400 when fullName is blank")
    void register_blankFullName_returns400() throws Exception {
        var request = new RegisterRequest("", "alice@example.com", "password1a", null);

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors").isArray());
    }

    @Test
    @DisplayName("POST /api/auth/register returns 400 when password has no digit")
    void register_noDigitPassword_returns400() throws Exception {
        var request = new RegisterRequest("Alice", "alice@example.com", "passwordonly", null);

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /api/auth/register returns 409 on duplicate username")
    void register_duplicateUsername_returns409() throws Exception {
        var request = new RegisterRequest("Alice", "alice@example.com", "password1a", null);
        when(authService.register(any())).thenThrow(new DuplicateResourceException("Username already registered"));

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("POST /api/auth/login returns 200 with message")
    void login_validCredentials_returns200() throws Exception {
        var request = new LoginRequest("alice@example.com", "password1a");
        when(authService.login(any(), any(), any())).thenReturn(AuthResponse.loggedIn());

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Login successful"));
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
        when(authService.logout(any())).thenReturn(AuthResponse.loggedOut());

        mockMvc.perform(post("/api/auth/logout"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Logout successful"));
    }

    @Test
    @DisplayName("POST /api/auth/logout returns 401 when not authenticated")
    void logout_unauthenticated_returns401() throws Exception {
        mockMvc.perform(post("/api/auth/logout"))
                .andExpect(status().isUnauthorized());
    }
}
