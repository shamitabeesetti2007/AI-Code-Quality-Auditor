
package com.auditor.auth.dto;

import com.auditor.user.AppUser;

import java.util.UUID;

public record UserResponse(
        UUID id,
        String email,
        String displayName,
        String role
) {
    public static UserResponse from(AppUser user) {
        return new UserResponse(
                user.getUserId(),
                user.getEmail(),
                user.getDisplayName(),
                user.getRole().name()
        );
    }
}

