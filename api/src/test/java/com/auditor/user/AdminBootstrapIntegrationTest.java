package com.auditor.user;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.web.server.ResponseStatusException;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
@Testcontainers
class AdminBootstrapIntegrationTest {

    private static final String TEST_DATABASE =
            "auditor_v3_flyway_test";

    @Container
    private static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>("postgres:16")
                    .withDatabaseName(TEST_DATABASE)
                    .withUsername("auditor")
                    .withPassword("auditor");

    @DynamicPropertySource
    static void configureDataSource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @Autowired
    private RoleManagementService roleManagementService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

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
        return userRepository.saveAndFlush(user);
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
