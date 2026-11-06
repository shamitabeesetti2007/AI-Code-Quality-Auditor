
package com.auditor.auth.token;

import com.auditor.auth.JwtService;
import com.auditor.user.AppUser;
import org.springframework.stereotype.Service;

import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.UUID;

@Service
public class RefreshTokenService {

    private static final long SLIDING_SECONDS = 7L * 24 * 60 * 60;
    private static final long FAMILY_SECONDS = 30L * 24 * 60 * 60;

    private final SecureRandom secureRandom = new SecureRandom();
    private final RefreshTokenTransactionService transactions;
    private final JwtService jwtService;

    public RefreshTokenService(
            RefreshTokenTransactionService transactions,
            JwtService jwtService) {
        this.transactions = transactions;
        this.jwtService = jwtService;
    }

    public IssuedToken issueInitial(AppUser user) {
        Instant now = Instant.now();
        Instant familyExpiry = now.plusSeconds(FAMILY_SECONDS);
        Instant expiry = now.plusSeconds(SLIDING_SECONDS);

        String rawToken = generateRawToken();

        transactions.createInitialToken(
                UUID.randomUUID(),
                user,
                UUID.randomUUID(),
                hash(rawToken),
                now,
                expiry,
                familyExpiry);

        return new IssuedToken(rawToken, expiry, familyExpiry);
    }

    public RotatedToken rotate(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) {
            throw new InvalidRefreshTokenException();
        }

        String successorRaw = generateRawToken();

        RefreshTokenTransactionService.RotationResult result =
                transactions.rotate(
                        hash(rawToken),
                        UUID.randomUUID(),
                        hash(successorRaw),
                        Instant.now(),
                        jwtService::generateAccessToken);

        if (result.status()
                != RefreshTokenTransactionService.RotationResult.Status.ROTATED) {
            // The worker transaction has returned, so reuse revocation
            // has already had the opportunity to commit.
            throw new InvalidRefreshTokenException();
        }

        return new RotatedToken(
                successorRaw,
                result.accessToken(),
                result.user(),
                result.expiresAt(),
                result.familyExpiresAt());
    }

    public void logout(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) {
            return;
        }

        transactions.logout(hash(rawToken), Instant.now());
    }

    private String generateRawToken() {
        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private byte[] hash(String rawToken) {
        try {
            return MessageDigest.getInstance("SHA-256")
                    .digest(rawToken.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        } catch (java.security.NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 is unavailable.", ex);
        }
    }

    public record IssuedToken(
            String rawToken,
            Instant expiresAt,
            Instant familyExpiresAt) {
    }

    public record RotatedToken(
            String rawToken,
            String accessToken,
            AppUser user,
            Instant expiresAt,
            Instant familyExpiresAt) {
    }
}
