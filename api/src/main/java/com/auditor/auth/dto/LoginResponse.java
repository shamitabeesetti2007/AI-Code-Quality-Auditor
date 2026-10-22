package com.auditor.auth.dto;

public record LoginResponse(
        String accessToken,
        String tokenType,
        long expiresIn,
        UserResponse user
) {
    public static LoginResponse from(String token, UserResponse user) {
        return new LoginResponse(token, "Bearer", 900, user);
    }
}