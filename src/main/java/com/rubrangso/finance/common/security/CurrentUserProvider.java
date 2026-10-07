package com.rubrangso.finance.common.security;

import com.rubrangso.finance.common.exception.ResourceNotFoundException;
import com.rubrangso.finance.user.User;
import com.rubrangso.finance.user.UserRepository;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 * Resolves the currently authenticated {@link User} from the {@code SecurityContext}.
 * Controllers and services call this instead of touching {@code SecurityContextHolder} directly.
 */
@Component
public class CurrentUserProvider {

    private final UserRepository userRepository;

    public CurrentUserProvider(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /**
     * Returns the {@link User} entity for the authenticated principal.
     *
     * @throws ResourceNotFoundException if the authenticated email has no matching user row
     */
    public User getCurrentUser() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Authenticated user not found"));
    }
}
