
package com.auditor.auth.token;

import com.auditor.user.AppUser;
import com.auditor.user.UserRepository;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.PreparedStatement;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.function.Function;

@Service
public class RefreshTokenTransactionService {

    private final RefreshTokenRepository tokenRepository;
    private final UserRepository userRepository;
    private final JdbcTemplate jdbcTemplate;

    public RefreshTokenTransactionService(
            RefreshTokenRepository tokenRepository,
            UserRepository userRepository,
            JdbcTemplate jdbcTemplate) {
        this.tokenRepository = tokenRepository;
        this.userRepository = userRepository;
        this.jdbcTemplate = jdbcTemplate;
    }

    @Transactional
    public void createInitialToken(
            UUID tokenId,
            AppUser user,
            UUID familyId,
            byte[] tokenHash,
            Instant now,
            Instant expiresAt,
            Instant familyExpiresAt) {

        tokenRepository.saveAndFlush(new RefreshToken(
                tokenId,
                user,
                familyId,
                tokenHash,
                now,
                expiresAt,
                familyExpiresAt));
    }

    @Transactional
    public RotationResult rotate(
            byte[] currentHash,
            UUID successorId,
            byte[] successorHash,
            Instant now,
            Function<AppUser, String> accessTokenGenerator) {

        // Fetch only the family ID. Do not load the token entity yet.
        UUID familyId = tokenRepository
                .findFamilyIdByTokenHash(currentHash)
                .orElse(null);

        if (familyId == null) {
            return RotationResult.invalid();
        }

        // All operations that mutate this family must acquire this
        // lock before acquiring any refresh-token row locks.
        lockFamily(familyId);

        // Read the token's current database state after acquiring
        // the family lock.
        RefreshToken current = tokenRepository
                .findByTokenHashForUpdate(currentHash)
                .orElse(null);

        if (current == null) {
            return RotationResult.invalid();
        }

        // A consumed token presented again is a replay.
        if (current.getUsedAt() != null
                || current.getReplacedBy() != null) {
            revokeFamily(familyId, now, "REUSE_DETECTED");
            return RotationResult.reused();
        }

        if (current.getRevokedAt() != null
                || !current.getExpiresAt().isAfter(now)
                || !current.getFamilyExpiresAt().isAfter(now)) {
            return RotationResult.invalid();
        }

        AppUser user = userRepository
                .findById(current.getUser().getUserId())
                .orElse(null);

        if (user == null) {
            return RotationResult.invalid();
        }

        if (user.getAccountStatus() != AppUser.AccountStatus.ACTIVE) {
            revokeFamily(familyId, now, "ACCOUNT_DISABLED");
            return RotationResult.invalid();
        }

        Instant familyExpiry = current.getFamilyExpiresAt();
        Instant slidingExpiry = now.plusSeconds(7L * 24 * 60 * 60);
        Instant successorExpiry = slidingExpiry.isBefore(familyExpiry)
                ? slidingExpiry
                : familyExpiry;

        RefreshToken successor = new RefreshToken(
                successorId,
                user,
                familyId,
                successorHash,
                now,
                successorExpiry,
                familyExpiry);

        // Insert successor and consume predecessor in the same
        // transaction.
        tokenRepository.saveAndFlush(successor);
        current.consume(now, successorId);
        String accessToken = accessTokenGenerator.apply(user);

        return RotationResult.rotated(
                accessToken,
                user,
                successorExpiry,
                familyExpiry);
    }

    @Transactional
    public void logout(byte[] tokenHash, Instant now) {

        // Again, discover the family without loading a token entity.
        UUID familyId = tokenRepository
                .findFamilyIdByTokenHash(tokenHash)
                .orElse(null);

        if (familyId == null) {
            return;
        }

        lockFamily(familyId);

        RefreshToken token = tokenRepository
                .findByTokenHashForUpdate(tokenHash)
                .orElse(null);

        if (token == null) {
            return;
        }

        revokeFamily(familyId, now, "LOGOUT");
    }

    /**
     * PostgreSQL transaction-scoped advisory lock.
     * A hash collision only causes unrelated families to serialize;
     * it does not compromise correctness.
     */
    private void lockFamily(UUID familyId) {
        jdbcTemplate.execute(
                (ConnectionCallback<Void>) connection -> {
                    try (PreparedStatement statement =
                                 connection.prepareStatement("""
                                     SELECT pg_advisory_xact_lock(
                                         hashtextextended(?::text, 0)
                                     )
                                     """)) {
                        statement.setString(1, familyId.toString());
                        statement.execute();
                    }
                    return null;
                });
    }

    private void revokeFamily(
            UUID familyId,
            Instant now,
            String reason) {

        // Caller must already hold the family advisory lock.
        List<RefreshToken> family =
                tokenRepository.findFamilyForUpdate(familyId);

        for (RefreshToken token : family) {
            token.revoke(now, reason);
        }
    }

    public record RotationResult(
            Status status,
            String accessToken,
            AppUser user,
            Instant expiresAt,
            Instant familyExpiresAt) {

        public enum Status {
            ROTATED,
            INVALID,
            REUSED
        }

        static RotationResult invalid() {
            return new RotationResult(
                    Status.INVALID, null, null, null, null);
        }

        static RotationResult reused() {
            return new RotationResult(
                    Status.REUSED, null, null, null, null);
        }

        static RotationResult rotated(
                String accessToken,
                AppUser user,
                Instant expiresAt,
                Instant familyExpiresAt) {
            return new RotationResult(
                    Status.ROTATED,
                    accessToken,
                    user,
                    expiresAt,
                    familyExpiresAt);
        }
    }
}
