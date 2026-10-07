package com.rubrangso.finance.auth.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.rubrangso.finance.user.User;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record AuthResponse(Long id, String name, String email, String phone) {

    public static AuthResponse from(User user) {
        return new AuthResponse(user.getId(), user.getName(), user.getEmail(), user.getPhone());
    }
}
