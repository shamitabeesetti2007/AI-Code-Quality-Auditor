
package com.auditor.auth;

import com.auditor.auth.dto.LoginResponse;

import java.time.Instant;

public record AuthResult(
        LoginResponse response,
        String refreshToken,
        Instant refreshTokenExpiresAt
) {
}

