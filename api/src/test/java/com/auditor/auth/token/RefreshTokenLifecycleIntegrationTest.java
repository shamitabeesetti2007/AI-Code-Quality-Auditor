
package com.auditor.auth.token;

import com.auditor.auth.JwtService;
import com.auditor.user.AppUser;
import com.auditor.user.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.support.EncodedResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.PreparedStatement;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

@SpringBootTest
class RefreshTokenLifecycleIntegrationTest {

    private static final String TEST_DATABASE =
            "auditor_v3_flyway_test";

    @Autowired
    private RefreshTokenService refreshTokenService;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @MockitoBean
    private JwtService jwtService;

    private AppUser user;

    @BeforeEach
    void setUp() {
        String database = jdbcTemplate.queryForObject(
                "SELECT current_database()", String.class);

        assertEquals(
                TEST_DATABASE,
                database,
                "Refusing to run against a database other than "
                        + TEST_DATABASE);

        user = userRepository.saveAndFlush(new AppUser(
                "refresh-test-" + UUID.randomUUID() + "@example.com",
                "test-password-hash",
                "Refresh Integration Test"));
        when(jwtService.generateAccessToken(any(AppUser.class)))
                .thenReturn("integration-access-token");
    }

    @AfterEach
    void cleanUp() {
        if (user == null || user.getUserId() == null) {
            return;
        }

        // Remove predecessor rows before successor rows to respect
        // the self-referencing replaced_by foreign key.
        List<UUID> tokenIds = jdbcTemplate.query(
                """
                SELECT refresh_token_id
                FROM refresh_token
                WHERE user_id = ?
                ORDER BY issued_at ASC, refresh_token_id ASC
                """,
                (rs, rowNum) ->
                        rs.getObject("refresh_token_id", UUID.class),
                user.getUserId());

        for (UUID tokenId : tokenIds) {
            jdbcTemplate.update(
                    "DELETE FROM refresh_token "
                            + "WHERE refresh_token_id = ?",
                    tokenId);
        }

        jdbcTemplate.update(
                "DELETE FROM app_user WHERE user_id = ?",
                user.getUserId());
    }

    @Test
    void initialTokenCanBeRotatedAndReplayRevokesEntireFamily() {
        RefreshTokenService.IssuedToken initial =
                refreshTokenService.issueInitial(user);

        UUID familyId = findFamilyId(initial.rawToken());

        RefreshTokenService.RotatedToken rotated =
                refreshTokenService.rotate(initial.rawToken());

        assertNotNull(rotated.rawToken());
        assertNotEquals(initial.rawToken(), rotated.rawToken());
        assertEquals(user.getUserId(), rotated.user().getUserId());
        assertEquals("integration-access-token", rotated.accessToken());

        assertEquals(
                initial.familyExpiresAt().toEpochMilli(),
                rotated.familyExpiresAt().toEpochMilli(),
                "Rotation should preserve absolute family expiry");

        assertTrue(rotated.expiresAt().isAfter(Instant.now()));
        assertFalse(
                rotated.expiresAt().isAfter(rotated.familyExpiresAt()));

        UUID successorId = jdbcTemplate.queryForObject(
                """
                SELECT replaced_by
                FROM refresh_token
                WHERE token_hash = ?
                """,
                UUID.class,
                sha256(initial.rawToken()));
        UUID predecessorId = jdbcTemplate.queryForObject(
                """
                SELECT refresh_token_id
                FROM refresh_token
                WHERE token_hash = ?
                """,
                UUID.class,
                sha256(initial.rawToken()));
        assertNotNull(successorId);
        assertNotNull(predecessorId);
        assertEquals(1, jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*)
                FROM refresh_token
                WHERE refresh_token_id = ?
                  AND used_at IS NOT NULL
                  AND replaced_by = ?
                """,
                Integer.class,
                predecessorId,
                successorId));
        assertEquals(1, countSuccessorTokens(successorId, familyId));
        assertEquals(1, countActiveSuccessors(successorId));

        assertThrows(
                InvalidRefreshTokenException.class,
                () -> refreshTokenService.rotate(initial.rawToken()));

        // Verify the family revocation committed before the exception.
        assertEquals(2, countFamilyTokens(familyId));
        assertEquals(2, countRevokedFamilyTokens(familyId));
        assertEquals(
                2,
                countFamilyTokensWithReason(familyId, "REUSE_DETECTED"));
    }

    @Test
    void accessTokenGenerationFailureRollsBackRotation() {
        RefreshTokenService.IssuedToken initial =
                refreshTokenService.issueInitial(user);

        UUID familyId = findFamilyId(initial.rawToken());
        doThrow(new IllegalStateException("JWT signing failed"))
                .when(jwtService)
                .generateAccessToken(any(AppUser.class));

        assertThrows(
                IllegalStateException.class,
                () -> refreshTokenService.rotate(initial.rawToken()));

        assertEquals(1, countFamilyTokens(familyId));
        assertEquals(0, countRevokedFamilyTokens(familyId));
        assertEquals(0, jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*)
                FROM refresh_token
                WHERE token_hash = ?
                  AND (used_at IS NOT NULL OR replaced_by IS NOT NULL)
                """,
                Integer.class,
                sha256(initial.rawToken())));
        assertEquals(0, jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*)
                FROM refresh_token
                WHERE family_id = ?
                  AND token_hash <> ?
                """,
                Integer.class,
                familyId,
                sha256(initial.rawToken())));
    }

    @Test
    void familyExpiryMigrationPreservesLegacyExpiryAndLifecycleState()
            throws Exception {
        ClassPathResource migration = new ClassPathResource(
                "db/migration/V4__refresh_token_family_expiry.sql");
        String migrationSql;
        try (var input = migration.getInputStream()) {
            migrationSql = new String(
                    input.readAllBytes(), StandardCharsets.UTF_8);
        }
        if (migrationSql.startsWith("\uFEFF")) {
            migrationSql = migrationSql.substring(1);
        }
        EncodedResource migrationResource = new EncodedResource(
                new ByteArrayResource(
                        migrationSql.getBytes(StandardCharsets.UTF_8)),
                StandardCharsets.UTF_8);

        jdbcTemplate.execute((org.springframework.jdbc.core.ConnectionCallback<Void>)
                connection -> {
                    try (var statement = connection.createStatement()) {
                        statement.execute("""
                                CREATE TEMPORARY TABLE refresh_token (
                                    refresh_token_id UUID PRIMARY KEY,
                                    family_id UUID NOT NULL,
                                    token_hash BYTEA NOT NULL,
                                    issued_at TIMESTAMPTZ NOT NULL,
                                    expires_at TIMESTAMPTZ NOT NULL,
                                    used_at TIMESTAMPTZ,
                                    revoked_at TIMESTAMPTZ,
                                    replaced_by UUID,
                                    revocation_reason VARCHAR(40)
                                )
                                """);
                    }

                    try {
                        UUID extendedFamily = UUID.randomUUID();
                        UUID ordinaryFamily = UUID.randomUUID();
                        UUID firstId = UUID.randomUUID();
                        UUID secondId = UUID.randomUUID();
                        UUID thirdId = UUID.randomUUID();
                        UUID ordinaryId = UUID.randomUUID();
                        Instant familyStart =
                                Instant.parse("2025-01-01T00:00:00Z");

                        try (PreparedStatement insert =
                                     connection.prepareStatement("""
                                             INSERT INTO refresh_token (
                                                 refresh_token_id,
                                                 family_id,
                                                 token_hash,
                                                 issued_at,
                                                 expires_at,
                                                 used_at,
                                                 revoked_at,
                                                 replaced_by,
                                                 revocation_reason
                                             ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                                             """)) {
                            insertLegacyToken(
                                    insert,
                                    firstId,
                                    extendedFamily,
                                    new byte[] {1},
                                    familyStart,
                                    familyStart.plusSeconds(7 * 86400L),
                                    familyStart.plusSeconds(7 * 86400L),
                                    null,
                                    secondId,
                                    null);
                            insertLegacyToken(
                                    insert,
                                    secondId,
                                    extendedFamily,
                                    new byte[] {2},
                                    familyStart.plusSeconds(7 * 86400L),
                                    familyStart.plusSeconds(14 * 86400L),
                                    familyStart.plusSeconds(14 * 86400L),
                                    familyStart.plusSeconds(14 * 86400L),
                                    thirdId,
                                    "REUSE_DETECTED");
                            insertLegacyToken(
                                    insert,
                                    thirdId,
                                    extendedFamily,
                                    new byte[] {3},
                                    familyStart.plusSeconds(14 * 86400L),
                                    familyStart.plusSeconds(35 * 86400L),
                                    null,
                                    familyStart.plusSeconds(15 * 86400L),
                                    null,
                                    "REUSE_DETECTED");
                            insertLegacyToken(
                                    insert,
                                    ordinaryId,
                                    ordinaryFamily,
                                    new byte[] {4},
                                    familyStart,
                                    familyStart.plusSeconds(7 * 86400L),
                                    null,
                                    null,
                                    null,
                                    null);
                        }

                        ScriptUtils.executeSqlScript(
                                connection, migrationResource);

                        try (var statement = connection.createStatement();
                             var results = statement.executeQuery("""
                                     SELECT family_id, family_expires_at
                                     FROM refresh_token
                                     WHERE refresh_token_id = '%s'
                                     """.formatted(firstId))) {
                            assertTrue(results.next());
                            assertEquals(extendedFamily,
                                    results.getObject("family_id", UUID.class));
                            assertEquals(
                                    familyStart.plusSeconds(35 * 86400L),
                                    results.getTimestamp(
                                            "family_expires_at").toInstant());
                        }

                        try (var statement = connection.createStatement();
                             var results = statement.executeQuery("""
                                     SELECT family_expires_at
                                     FROM refresh_token
                                     WHERE refresh_token_id = '%s'
                                     """.formatted(ordinaryId))) {
                            assertTrue(results.next());
                            assertEquals(
                                    familyStart.plusSeconds(30 * 86400L),
                                    results.getTimestamp(
                                            "family_expires_at").toInstant());
                        }

                        try (var statement = connection.createStatement();
                             var results = statement.executeQuery("""
                                     SELECT COUNT(*)
                                     FROM refresh_token
                                     WHERE family_id = '%s'
                                       AND (
                                           family_expires_at <= issued_at
                                           OR expires_at > family_expires_at
                                       )
                                     """.formatted(extendedFamily))) {
                            assertTrue(results.next());
                            assertEquals(0, results.getInt(1));
                        }

                        try (var statement = connection.createStatement();
                             var results = statement.executeQuery("""
                                     SELECT family_id, token_hash, issued_at,
                                            used_at, revoked_at, replaced_by,
                                            revocation_reason
                                     FROM refresh_token
                                     WHERE refresh_token_id = '%s'
                                     """.formatted(secondId))) {
                            assertTrue(results.next());
                            assertEquals(extendedFamily,
                                    results.getObject("family_id", UUID.class));
                            assertArrayEquals(new byte[] {2},
                                    results.getBytes("token_hash"));
                            assertEquals(
                                    familyStart.plusSeconds(7 * 86400L),
                                    results.getTimestamp("issued_at").toInstant());
                            assertEquals(
                                    familyStart.plusSeconds(14 * 86400L),
                                    results.getTimestamp("used_at").toInstant());
                            assertEquals(
                                    familyStart.plusSeconds(14 * 86400L),
                                    results.getTimestamp("revoked_at").toInstant());
                            assertEquals(
                                    thirdId,
                                    results.getObject("replaced_by", UUID.class));
                            assertEquals(
                                    "REUSE_DETECTED",
                                    results.getString("revocation_reason"));
                        }
                    } finally {
                        try (var statement = connection.createStatement()) {
                            statement.execute(
                                    "DROP TABLE IF EXISTS refresh_token");
                        }
                    }
                    return null;
                });
    }

    @Test
    void logoutRevokesEntireTokenFamily() {
        RefreshTokenService.IssuedToken initial =
                refreshTokenService.issueInitial(user);

        UUID familyId = findFamilyId(initial.rawToken());

        RefreshTokenService.RotatedToken rotated =
                refreshTokenService.rotate(initial.rawToken());

        refreshTokenService.logout(rotated.rawToken());

        assertEquals(2, countFamilyTokens(familyId));
        assertEquals(2, countRevokedFamilyTokens(familyId));
        assertEquals(2, countFamilyTokensWithReason(familyId, "LOGOUT"));

        assertDoesNotThrow(
                () -> refreshTokenService.logout("unknown-refresh-token"));
    }

    @Test
    void expiredTokenCannotBeRefreshed() {
        Instant now = Instant.now();
        UUID familyId = UUID.randomUUID();

        String expiredRawToken =
                "expired-test-token-" + UUID.randomUUID();

        Instant issuedAt = now.minusSeconds(10_000);
        Instant expiresAt = now.minusSeconds(1);
        Instant familyExpiresAt = now.plusSeconds(60_000);

        RefreshToken expiredToken = new RefreshToken(
                UUID.randomUUID(),
                user,
                familyId,
                sha256(expiredRawToken),
                issuedAt,
                expiresAt,
                familyExpiresAt);

        refreshTokenRepository.saveAndFlush(expiredToken);

        assertThrows(
                InvalidRefreshTokenException.class,
                () -> refreshTokenService.rotate(expiredRawToken));

        assertEquals(1, countFamilyTokens(familyId));
        assertEquals(0, countRevokedFamilyTokens(familyId));
    }

    @RepeatedTest(10)
    void concurrentRefreshAttemptsRevokeEntireFamily() throws Exception {
        RefreshTokenService.IssuedToken initial =
                refreshTokenService.issueInitial(user);

        UUID familyId = findFamilyId(initial.rawToken());

        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);

        try {
            Future<Boolean> first = executor.submit(() -> {
                ready.countDown();

                if (!start.await(10, TimeUnit.SECONDS)) {
                    throw new IllegalStateException(
                            "Timed out waiting for concurrent start");
                }

                try {
                    refreshTokenService.rotate(initial.rawToken());
                    return true;
                } catch (InvalidRefreshTokenException ex) {
                    return false;
                }
            });

            Future<Boolean> second = executor.submit(() -> {
                ready.countDown();

                if (!start.await(10, TimeUnit.SECONDS)) {
                    throw new IllegalStateException(
                            "Timed out waiting for concurrent start");
                }

                try {
                    refreshTokenService.rotate(initial.rawToken());
                    return true;
                } catch (InvalidRefreshTokenException ex) {
                    return false;
                }
            });

            assertTrue(
                    ready.await(10, TimeUnit.SECONDS),
                    "Both workers should be ready before starting");

            start.countDown();

            boolean firstSucceeded = first.get(20, TimeUnit.SECONDS);
            boolean secondSucceeded = second.get(20, TimeUnit.SECONDS);

            assertNotEquals(
                    firstSucceeded,
                    secondSucceeded,
                    "Exactly one refresh attempt should succeed");

            assertEquals(2, countFamilyTokens(familyId));
            assertEquals(2, countRevokedFamilyTokens(familyId));
            assertEquals(
                    2,
                    countFamilyTokensWithReason(
                            familyId, "REUSE_DETECTED"));
        } finally {
            start.countDown();
            executor.shutdownNow();

            assertTrue(
                    executor.awaitTermination(10, TimeUnit.SECONDS),
                    "Executor should terminate");
        }
    }

    @RepeatedTest(10)
    void concurrentRefreshAndLogoutLeaveFamilyFullyRevoked()
            throws Exception {

        RefreshTokenService.IssuedToken initial =
                refreshTokenService.issueInitial(user);

        UUID familyId = findFamilyId(initial.rawToken());

        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);

        try {
            Future<?> refresh = executor.submit(() -> {
                ready.countDown();
                awaitStart(start);

                try {
                    refreshTokenService.rotate(initial.rawToken());
                } catch (InvalidRefreshTokenException expected) {
                    // Logout may have won the race.
                }
            });

            Future<?> logout = executor.submit(() -> {
                ready.countDown();
                awaitStart(start);
                refreshTokenService.logout(initial.rawToken());
            });

            assertTrue(
                    ready.await(10, TimeUnit.SECONDS),
                    "Both workers should be ready before starting");

            start.countDown();

            // Surface worker exceptions and detect hangs/deadlocks.
            refresh.get(20, TimeUnit.SECONDS);
            logout.get(20, TimeUnit.SECONDS);

            int tokenCount = countFamilyTokens(familyId);

            assertTrue(
                    tokenCount >= 1,
                    "The initial token must remain recorded");

            assertEquals(
                    tokenCount,
                    countRevokedFamilyTokens(familyId),
                    "Every token in the family must be revoked");

            assertEquals(
                    tokenCount,
                    countFamilyTokensWithReason(familyId, "LOGOUT"),
                    "Logout should revoke the family with LOGOUT reason");
        } finally {
            start.countDown();
            executor.shutdownNow();

            assertTrue(
                    executor.awaitTermination(10, TimeUnit.SECONDS),
                    "Executor should terminate");
        }
    }

    private void awaitStart(CountDownLatch start) {
        try {
            if (!start.await(10, TimeUnit.SECONDS)) {
                throw new IllegalStateException(
                        "Timed out waiting for concurrent start");
            }
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(
                    "Interrupted while waiting to start", ex);
        }
    }

    @Test
    void disabledAccountCannotRefreshAndFamilyIsRevoked() {
        RefreshTokenService.IssuedToken initial =
                refreshTokenService.issueInitial(user);

        UUID familyId = findFamilyId(initial.rawToken());

        jdbcTemplate.update(
                """
                UPDATE app_user
                SET account_status = 'DISABLED'
                WHERE user_id = ?
                """,
                user.getUserId());

        assertThrows(
                InvalidRefreshTokenException.class,
                () -> refreshTokenService.rotate(initial.rawToken()));

        assertEquals(1, countFamilyTokens(familyId));
        assertEquals(
                1,
                countFamilyTokensWithReason(familyId, "ACCOUNT_DISABLED"));
    }

    private void insertLegacyToken(
            PreparedStatement insert,
            UUID tokenId,
            UUID familyId,
            byte[] tokenHash,
            Instant issuedAt,
            Instant expiresAt,
            Instant usedAt,
            Instant revokedAt,
            UUID replacedBy,
            String revocationReason) throws java.sql.SQLException {
        insert.setObject(1, tokenId);
        insert.setObject(2, familyId);
        insert.setBytes(3, tokenHash);
        insert.setObject(4, OffsetDateTime.ofInstant(issuedAt, ZoneOffset.UTC));
        insert.setObject(5, OffsetDateTime.ofInstant(expiresAt, ZoneOffset.UTC));
        insert.setObject(6, usedAt == null
                ? null
                : OffsetDateTime.ofInstant(usedAt, ZoneOffset.UTC));
        insert.setObject(7, revokedAt == null
                ? null
                : OffsetDateTime.ofInstant(revokedAt, ZoneOffset.UTC));
        insert.setObject(8, replacedBy);
        insert.setString(9, revocationReason);
        insert.executeUpdate();
    }

    private UUID findFamilyId(String rawToken) {
        List<UUID> familyIds = jdbcTemplate.query(
                """
                SELECT family_id
                FROM refresh_token
                WHERE token_hash = ?
                """,
                (rs, rowNum) ->
                        rs.getObject("family_id", UUID.class),
                sha256(rawToken));

        assertEquals(
                1,
                familyIds.size(),
                "Expected exactly one token matching the supplied token");

        return familyIds.get(0);
    }

    private int countFamilyTokens(UUID familyId) {
        Integer count = jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*)
                FROM refresh_token
                WHERE family_id = ?
                """,
                Integer.class,
                familyId);

        return count == null ? 0 : count;
    }

    private int countSuccessorTokens(UUID successorId, UUID familyId) {
        Integer count = jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*)
                FROM refresh_token
                WHERE refresh_token_id = ?
                  AND family_id = ?
                """,
                Integer.class,
                successorId,
                familyId);

        return count == null ? 0 : count;
    }

    private int countActiveSuccessors(UUID successorId) {
        Integer count = jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*)
                FROM refresh_token
                WHERE refresh_token_id = ?
                  AND used_at IS NULL
                  AND revoked_at IS NULL
                """,
                Integer.class,
                successorId);

        return count == null ? 0 : count;
    }

    private int countRevokedFamilyTokens(UUID familyId) {
        Integer count = jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*)
                FROM refresh_token
                WHERE family_id = ?
                  AND revoked_at IS NOT NULL
                """,
                Integer.class,
                familyId);

        return count == null ? 0 : count;
    }

    private int countFamilyTokensWithReason(
            UUID familyId,
            String reason) {

        Integer count = jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*)
                FROM refresh_token
                WHERE family_id = ?
                  AND revocation_reason = ?
                """,
                Integer.class,
                familyId,
                reason);

        return count == null ? 0 : count;
    }

    private byte[] sha256(String rawToken) {
        try {
            return MessageDigest.getInstance("SHA-256")
                    .digest(rawToken.getBytes(StandardCharsets.UTF_8));
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException(
                    "SHA-256 is unavailable.", ex);
        }
    }
}
