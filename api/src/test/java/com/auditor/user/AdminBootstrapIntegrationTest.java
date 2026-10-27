package com.auditor.user;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
class AdminBootstrapIntegrationTest {

    private static final String TEST_DATABASE =
            "auditor_v3_flyway_test";

    @Autowired
    private RoleManagementService roleManagementService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private final List<UUID> createdUserIds = new ArrayList<>();
    private String rollbackConstraint;

    @BeforeEach
    void verifyFreshBootstrapDatabase() {
        assertEquals(
                TEST_DATABASE,
                jdbcTemplate.queryForObject(
                        "SELECT current_database()", String.class));
        assertEquals(
                0,
                jdbcTemplate.queryForObject(
                        "SELECT COUNT(*) FROM app_user WHERE role = 'ADMIN'",
                        Integer.class));
        assertEquals(
                0,
                jdbcTemplate.queryForObject(
                        "SELECT COUNT(*) FROM audit_log "
                                + "WHERE event_type = 'ADMIN_BOOTSTRAP'",
                        Integer.class),
                "Bootstrap integration test requires a fresh disposable database.");
    }

    @AfterEach
    void cleanUpUserFixtures() {
        if (rollbackConstraint != null) {
            jdbcTemplate.execute(
                    "ALTER TABLE audit_log DROP CONSTRAINT IF EXISTS "
                            + rollbackConstraint);
            rollbackConstraint = null;
        }
        for (UUID userId : createdUserIds) {
            jdbcTemplate.update(
                    """
                    UPDATE app_user
                    SET role = 'USER', account_status = 'ACTIVE'
                    WHERE user_id = ?
                    """,
                    userId);
        }
        createdUserIds.clear();
    }

    @Test
    void bootstrapIsAtomicSingleUseAndAudited() {
        AppUser firstTarget = createUser();
        AppUser secondTarget = createUser();

        rollbackConstraint = "ck_test_reject_bootstrap_audit_"
                + UUID.randomUUID().toString().replace("-", "");
        jdbcTemplate.execute(
                "ALTER TABLE audit_log ADD CONSTRAINT "
                        + rollbackConstraint
                        + " CHECK (event_type <> 'ADMIN_BOOTSTRAP')");

        assertThrows(
                DataIntegrityViolationException.class,
                () -> roleManagementService.bootstrapFirstAdmin(
                        firstTarget.getUserId(),
                        "Initial production setup"));
        assertEquals(
                "USER",
                roleFor(firstTarget.getUserId()));
        assertEquals(
                0,
                bootstrapAuditCount(firstTarget.getUserId()));

        jdbcTemplate.execute(
                "ALTER TABLE audit_log DROP CONSTRAINT "
                        + rollbackConstraint);
        rollbackConstraint = null;

        jdbcTemplate.update(
                "UPDATE app_user SET account_status = 'DISABLED' "
                        + "WHERE user_id = ?",
                secondTarget.getUserId());
        assertThrows(
                ResponseStatusException.class,
                () -> roleManagementService.bootstrapFirstAdmin(
                        secondTarget.getUserId(),
                        "Initial production setup"));
        assertEquals("USER", roleFor(secondTarget.getUserId()));

        jdbcTemplate.update(
                "UPDATE app_user SET account_status = 'ACTIVE' "
                        + "WHERE user_id = ?",
                secondTarget.getUserId());
        roleManagementService.bootstrapFirstAdmin(
                firstTarget.getUserId(),
                " Initial production setup ");

        assertEquals("ADMIN", roleFor(firstTarget.getUserId()));
        assertEquals(1, bootstrapAuditCount(firstTarget.getUserId()));
        assertEquals(
                1,
                jdbcTemplate.queryForObject(
                        """
                        SELECT COUNT(*)
                        FROM audit_log
                        WHERE actor_user_id IS NULL
                          AND target_user_id = ?
                          AND event_type = 'ADMIN_BOOTSTRAP'
                          AND old_role IS NULL
                          AND new_role = 'ADMIN'
                          AND metadata ->> 'reason' = 'Initial production setup'
                        """,
                        Integer.class,
                        firstTarget.getUserId()));

        assertThrows(
                ResponseStatusException.class,
                () -> roleManagementService.bootstrapFirstAdmin(
                        secondTarget.getUserId(),
                        "Attempted second bootstrap"));
        assertEquals("USER", roleFor(secondTarget.getUserId()));
        assertEquals(1, bootstrapAuditCount(firstTarget.getUserId()));
    }

    private AppUser createUser() {
        AppUser user = new AppUser(
                "bootstrap-it-" + UUID.randomUUID() + "@example.com",
                "test-password-hash",
                "Bootstrap integration test");
        AppUser saved = userRepository.saveAndFlush(user);
        createdUserIds.add(saved.getUserId());
        return saved;
    }

    private String roleFor(UUID userId) {
        return jdbcTemplate.queryForObject(
                "SELECT role FROM app_user WHERE user_id = ?",
                String.class,
                userId);
    }

    private int bootstrapAuditCount(UUID userId) {
        return jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*)
                FROM audit_log
                WHERE event_type = 'ADMIN_BOOTSTRAP'
                  AND target_user_id = ?
                """,
                Integer.class,
                userId);
    }
}
