package com.rubrangso.finance.auth.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.rubrangso.finance.user.User;

/** Returned on auth operations. {@code userId} is omitted for login/logout responses. */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record AuthResponse(String message, Long userId) {

    public static AuthResponse registered(User user) {
        return new AuthResponse("User registered successfully", user.getId());
    }

    public static AuthResponse loggedIn() {
        return new AuthResponse("Login successful", null);
    }

    public static AuthResponse loggedOut() {
        return new AuthResponse("Logout successful", null);
    }
}
