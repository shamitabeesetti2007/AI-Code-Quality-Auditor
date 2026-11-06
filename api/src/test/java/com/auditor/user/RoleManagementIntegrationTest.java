package com.auditor.user;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
class RoleManagementIntegrationTest {

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
    void verifyDisposableDatabase() {
        assertEquals(
                TEST_DATABASE,
                jdbcTemplate.queryForObject(
                        "SELECT current_database()", String.class),
                "Refusing to run against a database other than "
                        + TEST_DATABASE);
        assertEquals(
                0,
                jdbcTemplate.queryForObject(
                        """
                        SELECT COUNT(*)
                        FROM app_user
                        WHERE role = 'ADMIN'
                          AND account_status = 'ACTIVE'
                        """,
                        Integer.class),
                "Role integration tests require a clean disposable database.");
    }

    @AfterEach
    void cleanUpRoleFixtures() {
        if (rollbackConstraint != null) {
            jdbcTemplate.execute(
                    "ALTER TABLE audit_log DROP CONSTRAINT IF EXISTS "
                            + rollbackConstraint);
            rollbackConstraint = null;
        }

        for (UUID userId : createdUserIds) {
            jdbcTemplate.update(
                    "UPDATE app_user SET role = 'USER' WHERE user_id = ?",
                    userId);
        }
        createdUserIds.clear();
    }

    @Test
    void roleChangeAndAuditPersistTogether() {
        AppUser admin = createUser(AppUser.Role.ADMIN);
        AppUser target = createUser(AppUser.Role.USER);

        roleManagementService.changeRole(
                admin.getUserId(),
                target.getUserId(),
                AppUser.Role.RESEARCHER);

        assertEquals(
                "RESEARCHER",
                jdbcTemplate.queryForObject(
                        "SELECT role FROM app_user WHERE user_id = ?",
                        String.class,
                        target.getUserId()));
        assertEquals(
                1,
                jdbcTemplate.queryForObject(
                        """
                        SELECT COUNT(*)
                        FROM audit_log
                        WHERE actor_user_id = ?
                          AND target_user_id = ?
                          AND event_type = 'ROLE_CHANGED'
                          AND old_role = 'USER'
                          AND new_role = 'RESEARCHER'
                          AND metadata = '{}'::JSONB
                        """,
                        Integer.class,
                        admin.getUserId(),
                        target.getUserId()));
    }

    @Test
    void auditInsertFailureRollsBackRoleUpdate() {
        AppUser admin = createUser(AppUser.Role.ADMIN);
        AppUser target = createUser(AppUser.Role.USER);
        rollbackConstraint = "ck_test_reject_role_audit_"
                + UUID.randomUUID().toString().replace("-", "");
        jdbcTemplate.execute(
                "ALTER TABLE audit_log ADD CONSTRAINT "
                        + rollbackConstraint
                        + " CHECK (target_user_id IS DISTINCT FROM '"
                        + target.getUserId()
                        + "'::UUID)");

        assertThrows(
                DataIntegrityViolationException.class,
                () -> roleManagementService.changeRole(
                        admin.getUserId(),
                        target.getUserId(),
                        AppUser.Role.RESEARCHER));

        assertEquals(
                "USER",
                jdbcTemplate.queryForObject(
                        "SELECT role FROM app_user WHERE user_id = ?",
                        String.class,
                        target.getUserId()));
        assertEquals(
                0,
                jdbcTemplate.queryForObject(
                        """
                        SELECT COUNT(*)
                        FROM audit_log
                        WHERE target_user_id = ?
                          AND event_type = 'ROLE_CHANGED'
                        """,
                        Integer.class,
                        target.getUserId()));
    }

    @Test
    void concurrentDemotionsCannotRemoveEveryActiveAdministrator()
            throws Exception {
        AppUser firstAdmin = createUser(AppUser.Role.ADMIN);
        AppUser secondAdmin = createUser(AppUser.Role.ADMIN);
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);

        try {
            Future<Boolean> first = executor.submit(() -> attemptDemotion(
                    firstAdmin.getUserId(),
                    secondAdmin.getUserId(),
                    ready,
                    start));
            Future<Boolean> second = executor.submit(() -> attemptDemotion(
                    secondAdmin.getUserId(),
                    firstAdmin.getUserId(),
                    ready,
                    start));

            assertTrue(ready.await(10, TimeUnit.SECONDS));
            start.countDown();

            boolean firstSucceeded = first.get(20, TimeUnit.SECONDS);
            boolean secondSucceeded = second.get(20, TimeUnit.SECONDS);
            assertTrue(firstSucceeded ^ secondSucceeded);
        } finally {
            start.countDown();
            executor.shutdownNow();
            assertTrue(executor.awaitTermination(10, TimeUnit.SECONDS));
        }

        assertEquals(
                1,
                jdbcTemplate.queryForObject(
                        """
                        SELECT COUNT(*)
                        FROM app_user
                        WHERE user_id IN (?, ?)
                          AND role = 'ADMIN'
                          AND account_status = 'ACTIVE'
                        """,
                        Integer.class,
                        firstAdmin.getUserId(),
                        secondAdmin.getUserId()));
        assertEquals(
                1,
                jdbcTemplate.queryForObject(
                        """
                        SELECT COUNT(*)
                        FROM audit_log
                        WHERE event_type = 'ROLE_CHANGED'
                          AND target_user_id IN (?, ?)
                        """,
                        Integer.class,
                        firstAdmin.getUserId(),
                        secondAdmin.getUserId()));
    }

    private boolean attemptDemotion(
            UUID actorId,
            UUID targetId,
            CountDownLatch ready,
            CountDownLatch start) throws InterruptedException {
        ready.countDown();
        if (!start.await(10, TimeUnit.SECONDS)) {
            throw new IllegalStateException(
                    "Timed out waiting to start concurrent role change.");
        }

        try {
            roleManagementService.changeRole(
                    actorId, targetId, AppUser.Role.USER);
            return true;
        } catch (ResponseStatusException exception) {
            if (exception.getStatusCode() == HttpStatus.FORBIDDEN
                    || exception.getStatusCode() == HttpStatus.CONFLICT) {
                return false;
            }
            throw exception;
        }
    }

    private AppUser createUser(AppUser.Role role) {
        AppUser user = new AppUser(
                "role-it-" + UUID.randomUUID() + "@example.com",
                "test-password-hash",
                "Role integration test");
        if (role != AppUser.Role.USER) {
            user.changeRole(role);
        }
        AppUser saved = userRepository.saveAndFlush(user);
        createdUserIds.add(saved.getUserId());
        return saved;
    }
}
