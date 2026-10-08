package com.rubrangso.finance.auth;

import com.rubrangso.finance.auth.dto.AuthResponse;
import com.rubrangso.finance.auth.dto.LoginRequest;
import com.rubrangso.finance.auth.dto.RegisterRequest;
import com.rubrangso.finance.common.exception.DuplicateResourceException;
import com.rubrangso.finance.user.User;
import com.rubrangso.finance.user.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.stereotype.Service;

/**
 * Handles registration, login, and logout.
 * Login explicitly saves the {@link SecurityContext} to the session so that
 * Spring Security 6's opt-in context persistence is satisfied.
 */
@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final HttpSessionSecurityContextRepository securityContextRepository;

    public AuthService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            @Lazy AuthenticationManager authenticationManager,
            HttpSessionSecurityContextRepository securityContextRepository) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.securityContextRepository = securityContextRepository;
    }

    /**
     * Registers a new user. {@code username} is treated as an email address.
     *
     * @throws DuplicateResourceException if the username (email) is already taken
     */
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.username())) {
            throw new DuplicateResourceException("Username already registered");
        }
        var user = new User(
                request.fullName(),
                request.username(),
                passwordEncoder.encode(request.password()),
                request.phoneNumber());
        return AuthResponse.registered(userRepository.save(user));
    }

    /**
     * Authenticates the user, rotates the session ID to prevent fixation, and persists
     * the {@link SecurityContext} so subsequent requests are recognized as authenticated.
     */
    public AuthResponse login(
            LoginRequest request,
            HttpServletRequest httpRequest,
            HttpServletResponse httpResponse) {

        var authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.username(), request.password()));

        // Ensure a session exists, then rotate its ID (session fixation protection)
        httpRequest.getSession(true);
        httpRequest.changeSessionId();

        // Persist security context — required in Spring Security 6
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);
        securityContextRepository.saveContext(context, httpRequest, httpResponse);

        return AuthResponse.loggedIn();
    }

    /**
     * Invalidates the current session and clears the security context.
     * If the request reaches this method the caller is already authenticated
     * (unauthenticated requests are rejected by the security filter with 401).
     */
    public AuthResponse logout(HttpServletRequest request) {
        var session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
        SecurityContextHolder.clearContext();
        return AuthResponse.loggedOut();
    }
}
