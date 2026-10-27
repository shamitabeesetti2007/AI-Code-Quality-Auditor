package com.auditor.audit;

import com.auditor.user.AppUser;

import java.util.Map;
import java.util.UUID;

public record AuditEvent(
        UUID auditId,
        UUID actorUserId,
        UUID targetUserId,
        EventType eventType,
        AppUser.Role oldRole,
        AppUser.Role newRole,
        Map<String, Object> metadata) {

    public enum EventType {
        ROLE_CHANGED,
        ADMIN_BOOTSTRAP
    }
}
