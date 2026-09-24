package com.prakalpa.api.services;


import com.prakalpa.api.config.CustomUserDetails;
import com.prakalpa.api.models.Users;
import com.prakalpa.api.repository.UserRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
public class AuthUserService {

    private final UserRepository userRepository;

    public AuthUserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /**
     * Get the ID of the currently logged-in user directly from Security Context.
     */
    public Long getLoggedInUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()) {
            throw new IllegalStateException("No authenticated user found in security context");
        }

        Object principal = authentication.getPrincipal();

        // 1. If Principal is CustomUserDetails
        if (principal instanceof CustomUserDetails userDetails) {
            return userDetails.getUserDetails().getId();
        }

        // 2. Fallback: If Principal is a plain username string from JWT claims
        if (principal instanceof String username) {
            return userRepository.findByUsername(username)
                    .map(Users::getId)
                    .orElseThrow(() -> new RuntimeException("User not found: " + username));
        }

        throw new IllegalStateException("Unexpected principal type: " + principal.getClass());
    }

    /**
     * Get the full Users entity of the currently logged-in user.
     */
    public Users getLoggedInUser() {
        Long userId = getLoggedInUserId();
        return userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Authenticated user ID not found in database: " + userId));
    }
}