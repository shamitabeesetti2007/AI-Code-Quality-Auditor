
package com.auditor.auth.token;

import com.auditor.user.AppUser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class RefreshTokenTest {

    private AppUser user;
    private UUID tokenId;
    private UUID familyId;
    private Instant issuedAt;
    private RefreshToken token;

    @BeforeEach
    void setUp() {
        user = new AppUser(
                "refresh-test@example.com",
                "test-password-hash",
                "Refresh Test"
        );

        tokenId = UUID.randomUUID();
        familyId = UUID.randomUUID();
        issuedAt = Instant.parse("2026-01-01T00:00:00Z");

        token = new RefreshToken(
                tokenId,
                user,
                familyId,
                new byte[32],
                issuedAt,
                issuedAt.plusSeconds(7L * 24 * 60 * 60),
                issuedAt.plusSeconds(30L * 24 * 60 * 60)
        );
    }

    @Test
    void newTokenStartsUnusedAndUnrevoked() {
        assertNull(token.getUsedAt());
        assertNull(token.getReplacedBy());
        assertNull(token.getRevokedAt());
        assertNull(token.getRevocationReason());
    }

    @Test
    void consumeRecordsTimestampAndSuccessor() {
        Instant consumedAt = issuedAt.plusSeconds(60);
        UUID successorId = UUID.randomUUID();

        token.consume(consumedAt, successorId);

        assertEquals(consumedAt, token.getUsedAt());
        assertEquals(successorId, token.getReplacedBy());
    }

    @Test
    void consumingTokenTwiceIsRejected() {
        token.consume(issuedAt.plusSeconds(60), UUID.randomUUID());

        assertThrows(
                IllegalStateException.class,
                () -> token.consume(
                        issuedAt.plusSeconds(120),
                        UUID.randomUUID()
                )
        );
    }

    @Test
    void consumingWithNullSuccessorIsRejected() {
        assertThrows(
                IllegalArgumentException.class,
                () -> token.consume(issuedAt, null)
        );
    }

    @Test
    void consumingWithSameTokenIdIsRejected() {
        assertThrows(
                IllegalArgumentException.class,
                () -> token.consume(issuedAt, tokenId)
        );
    }

    @Test
    void revokeRecordsFirstRevocationReason() {
        Instant revokedAt = issuedAt.plusSeconds(60);

        token.revoke(revokedAt, "LOGOUT");
        token.revoke(revokedAt.plusSeconds(60), "REUSE_DETECTED");

        assertEquals(revokedAt, token.getRevokedAt());
        assertEquals("LOGOUT", token.getRevocationReason());
    }

    @Test
    void blankRevocationReasonIsRejected() {
        assertThrows(
                IllegalArgumentException.class,
                () -> token.revoke(issuedAt, " ")
        );
    }

    @Test
    void tokenHashIsDefensivelyCopied() {
        byte[] originalHash = new byte[32];
        originalHash[0] = 10;

        RefreshToken separateToken = new RefreshToken(
                UUID.randomUUID(),
                user,
                familyId,
                originalHash,
                issuedAt,
                issuedAt.plusSeconds(100),
                issuedAt.plusSeconds(200)
        );

        originalHash[0] = 99;
        assertEquals(10, separateToken.getTokenHash()[0]);

        byte[] returnedHash = separateToken.getTokenHash();
        returnedHash[0] = 88;

        assertEquals(10, separateToken.getTokenHash()[0]);
    }
}

