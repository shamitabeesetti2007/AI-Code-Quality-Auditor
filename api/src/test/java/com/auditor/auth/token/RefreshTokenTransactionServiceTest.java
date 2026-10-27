package com.auditor.auth.token;

import com.auditor.user.AppUser;
import com.auditor.user.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RefreshTokenTransactionServiceTest {

    @Mock
    private RefreshTokenRepository tokenRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private JdbcTemplate jdbcTemplate;

    @Test
    void expiredFamilyRejectsUnexpiredTokenWithoutSavingSuccessor() {
        // Unit-level state only: the database constraint
        // expires_at <= family_expires_at makes this combination
        // unpersistable under normal schema constraints.
        Instant now = Instant.now();
        UUID familyId = UUID.randomUUID();
        byte[] currentHash = {1, 2, 3};
        RefreshToken current = new RefreshToken(
                UUID.randomUUID(),
                new AppUser(
                        "refresh-transaction-test@example.com",
                        "test-password-hash",
                        "Refresh Transaction Test"),
                familyId,
                currentHash,
                now.minusSeconds(60),
                now.plusSeconds(60),
                now.minusSeconds(1));

        assertTrue(current.getExpiresAt().isAfter(now));
        assertTrue(current.getFamilyExpiresAt().isBefore(now));

        when(tokenRepository.findFamilyIdByTokenHash(currentHash))
                .thenReturn(Optional.of(familyId));
        when(tokenRepository.findByTokenHashForUpdate(currentHash))
                .thenReturn(Optional.of(current));
        when(jdbcTemplate.<Void>execute(any(ConnectionCallback.class)))
                .thenReturn(null);

        RefreshTokenTransactionService transactions =
                new RefreshTokenTransactionService(
                        tokenRepository,
                        userRepository,
                        jdbcTemplate);

        RefreshTokenTransactionService.RotationResult result =
                transactions.rotate(
                        currentHash,
                        UUID.randomUUID(),
                        new byte[] {4, 5, 6},
                        now,
                        ignored -> "access-token");

        assertEquals(
                RefreshTokenTransactionService.RotationResult.Status.INVALID,
                result.status());
        assertNull(current.getUsedAt());
        assertNull(current.getReplacedBy());
        verify(tokenRepository, never())
                .saveAndFlush(any(RefreshToken.class));
        verify(userRepository, never()).findById(current.getUser().getUserId());
    }
}
