package com.auditor.audit;

import com.auditor.user.AppUser;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.Map;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class AuditEventRepositoryTest {

    @Test
    void insertsRoleChangeWithJsonbMetadata() {
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        AuditEventRepository repository = new AuditEventRepository(jdbcTemplate);
        UUID auditId = UUID.randomUUID();
        UUID actorId = UUID.randomUUID();
        UUID targetId = UUID.randomUUID();

        repository.insert(new AuditEvent(
                auditId,
                actorId,
                targetId,
                AuditEvent.EventType.ROLE_CHANGED,
                AppUser.Role.USER,
                AppUser.Role.ADMIN,
                Map.of("source", "admin-api")));

        verify(jdbcTemplate).update(
                contains("CAST(? AS JSONB)"),
                eq(auditId),
                eq(actorId),
                eq(targetId),
                eq("ROLE_CHANGED"),
                eq("USER"),
                eq("ADMIN"),
                eq("{\"source\":\"admin-api\"}"));
    }

    @Test
    void insertsBootstrapAuditWithoutAnActorOrOldRole() {
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        AuditEventRepository repository = new AuditEventRepository(jdbcTemplate);
        UUID auditId = UUID.randomUUID();
        UUID targetId = UUID.randomUUID();

        repository.insert(new AuditEvent(
                auditId,
                null,
                targetId,
                AuditEvent.EventType.ADMIN_BOOTSTRAP,
                null,
                AppUser.Role.ADMIN,
                Map.of("reason", "Initial production setup")));

        verify(jdbcTemplate).update(
                contains("CAST(? AS JSONB)"),
                eq(auditId),
                isNull(),
                eq(targetId),
                eq("ADMIN_BOOTSTRAP"),
                isNull(),
                eq("ADMIN"),
                eq("{\"reason\":\"Initial production setup\"}"));
    }
}
