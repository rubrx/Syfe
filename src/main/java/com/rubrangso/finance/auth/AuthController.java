package com.rubrangso.finance.auth;

import com.rubrangso.finance.auth.dto.AuthResponse;
import com.rubrangso.finance.auth.dto.LoginRequest;
import com.rubrangso.finance.auth.dto.RegisterRequest;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Authentication endpoints: register, login, logout. */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    /**
     * Registers a new user account.
     *
     * @return the created user (no password)
     */
    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public AuthResponse register(@Valid @RequestBody RegisterRequest request) {
        return authService.register(request);
    }

    /**
     * Authenticates the user and starts a session. The {@code JSESSIONID} cookie is
     * set {@code HttpOnly} and {@code SameSite=Lax} (see {@code application.yml}).
     */
    @PostMapping("/login")
    public AuthResponse login(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest httpRequest,
            HttpServletResponse httpResponse) {
        return authService.login(request, httpRequest, httpResponse);
    }

    /**
     * Ends the current session. An unauthenticated caller never reaches this method —
     * the security filter returns 401 before routing.
     */
    @PostMapping("/logout")
    public Map<String, String> logout(HttpServletRequest request) {
        authService.logout(request);
        return Map.of("message", "Logged out successfully");
    }
}
