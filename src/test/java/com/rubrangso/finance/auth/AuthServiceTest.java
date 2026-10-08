package com.rubrangso.finance.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.rubrangso.finance.auth.dto.LoginRequest;
import com.rubrangso.finance.auth.dto.RegisterRequest;
import org.springframework.test.util.ReflectionTestUtils;
import com.rubrangso.finance.common.exception.DuplicateResourceException;
import com.rubrangso.finance.user.User;
import com.rubrangso.finance.user.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private AuthenticationManager authenticationManager;
    @Mock private HttpSessionSecurityContextRepository securityContextRepository;
    @Mock private HttpServletRequest httpRequest;
    @Mock private HttpServletResponse httpResponse;

    private AuthService authService;

    @BeforeEach
    void setUp() {
        authService = new AuthService(
                userRepository, passwordEncoder, authenticationManager, securityContextRepository);
    }

    @Test
    @DisplayName("register_newUsername_returnsRegisteredResponse")
    void register_newUsername_returnsRegisteredResponse() {
        var request = new RegisterRequest("Alice", "alice@example.com", "password1", null);
        var saved = new User("Alice", "alice@example.com", "hashed", null);
        ReflectionTestUtils.setField(saved, "id", 1L);

        when(userRepository.existsByEmail("alice@example.com")).thenReturn(false);
        when(passwordEncoder.encode("password1")).thenReturn("hashed");
        when(userRepository.save(any(User.class))).thenReturn(saved);

        var response = authService.register(request);

        assertThat(response.message()).isEqualTo("User registered successfully");
        assertThat(response.userId()).isNotNull();
    }

    @Test
    @DisplayName("register_duplicateUsername_throwsDuplicateResourceException")
    void register_duplicateUsername_throwsDuplicateResourceException() {
        var request = new RegisterRequest("Alice", "alice@example.com", "password1", null);
        when(userRepository.existsByEmail("alice@example.com")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(DuplicateResourceException.class);
    }

    @Test
    @DisplayName("login_validCredentials_savesContextAndReturnsLoginResponse")
    void login_validCredentials_savesContextAndReturnsLoginResponse() {
        var request = new LoginRequest("alice@example.com", "password1");
        Authentication auth = mock(Authentication.class);
        HttpSession session = mock(HttpSession.class);

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(auth);
        when(httpRequest.getSession(true)).thenReturn(session);

        var response = authService.login(request, httpRequest, httpResponse);

        assertThat(response.message()).isEqualTo("Login successful");
        verify(httpRequest).changeSessionId();
        verify(securityContextRepository).saveContext(any(), any(), any());
    }

    @Test
    @DisplayName("login_badCredentials_propagatesException")
    void login_badCredentials_propagatesException() {
        var request = new LoginRequest("alice@example.com", "wrong");
        when(authenticationManager.authenticate(any()))
                .thenThrow(new BadCredentialsException("Bad credentials"));

        assertThatThrownBy(() -> authService.login(request, httpRequest, httpResponse))
                .isInstanceOf(BadCredentialsException.class);
    }

    @Test
    @DisplayName("logout_withSession_invalidatesSession")
    void logout_withSession_invalidatesSession() {
        HttpSession session = mock(HttpSession.class);
        when(httpRequest.getSession(false)).thenReturn(session);

        var response = authService.logout(httpRequest);

        verify(session).invalidate();
        assertThat(response.message()).isEqualTo("Logout successful");
    }

    @Test
    @DisplayName("logout_withoutSession_doesNotThrow")
    void logout_withoutSession_doesNotThrow() {
        when(httpRequest.getSession(false)).thenReturn(null);

        var response = authService.logout(httpRequest);

        assertThat(response.message()).isEqualTo("Logout successful");
    }
}
