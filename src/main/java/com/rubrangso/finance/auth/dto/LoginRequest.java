package com.rubrangso.finance.auth.dto;

import jakarta.validation.constraints.NotBlank;

/** Request body for POST /api/auth/login. */
public record LoginRequest(

        @NotBlank(message = "Username is required")
        String username,

        @NotBlank(message = "Password is required")
        String password
) {}
