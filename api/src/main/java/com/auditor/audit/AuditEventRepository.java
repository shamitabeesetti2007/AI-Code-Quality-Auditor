package com.auditor.audit;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class AuditEventRepository {

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public AuditEventRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public boolean adminBootstrapExists() {
        return Boolean.TRUE.equals(jdbcTemplate.queryForObject(
                """
                SELECT EXISTS (
                    SELECT 1
                    FROM audit_log
                    WHERE event_type = 'ADMIN_BOOTSTRAP'
                )
                """,
                Boolean.class));
    }

    public void insert(AuditEvent event) {
        String metadata;
        try {
            metadata = objectMapper.writeValueAsString(event.metadata());
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException(
                    "Could not serialize audit metadata.", ex);
        }

        jdbcTemplate.update("""
                INSERT INTO audit_log (
                    audit_id,
                    actor_user_id,
                    target_user_id,
                    event_type,
                    old_role,
                    new_role,
                    metadata
                )
                VALUES (?, ?, ?, ?, ?, ?, CAST(? AS JSONB))
                """,
                event.auditId(),
                event.actorUserId(),
                event.targetUserId(),
                event.eventType().name(),
                event.oldRole() == null ? null : event.oldRole().name(),
                event.newRole() == null ? null : event.newRole().name(),
                metadata);
    }
}
