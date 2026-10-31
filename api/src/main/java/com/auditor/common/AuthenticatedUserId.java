package com.auditor.common;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

public final class AuthenticatedUserId {

    private AuthenticatedUserId() {
    }

    public static UUID from(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED, "Authentication required.");
        }
        try {
            return UUID.fromString(authentication.getName());
        } catch (IllegalArgumentException | NullPointerException ex) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED, "Invalid authentication.");
        }
    }
}
