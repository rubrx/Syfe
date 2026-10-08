package com.rubrangso.finance.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Request body for POST /api/auth/register. */
public record RegisterRequest(

        @NotBlank(message = "Full name is required")
        String fullName,

        @NotBlank(message = "Username is required")
        @Email(message = "Username must be a valid email address")
        String username,

        @NotBlank(message = "Password is required")
        @Size(min = 8, message = "Password must be at least 8 characters")
        @Pattern(
                regexp = "^(?=.*[A-Za-z])(?=.*\\d).{8,}$",
                message = "Password must contain at least one letter and one digit")
        String password,

        @Pattern(regexp = "^\\+?[0-9]{7,15}$", message = "Phone must be 7-15 digits with optional + prefix")
        String phoneNumber
) {}
