package com.auditor.user;

import com.auditor.audit.AuditEvent;
import com.auditor.audit.AuditEventRepository;
import com.auditor.auth.dto.UserResponse;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;
import java.util.UUID;

@Service
public class RoleManagementService {

    private static final int ROLE_LOCK_NAMESPACE = 0x415544;
    private static final int ROLE_LOCK_KEY = 1;

    private final UserRepository userRepository;
    private final AuditEventRepository auditEventRepository;
    private final JdbcTemplate jdbcTemplate;

    public RoleManagementService(
            UserRepository userRepository,
            AuditEventRepository auditEventRepository,
            JdbcTemplate jdbcTemplate) {
        this.userRepository = userRepository;
        this.auditEventRepository = auditEventRepository;
        this.jdbcTemplate = jdbcTemplate;
    }

    @Transactional
    public UserResponse changeRole(
            UUID actorUserId,
            UUID targetUserId,
            AppUser.Role requestedRole) {
        lockRoleChanges();

        AppUser actor = userRepository.findByIdForUpdate(actorUserId)
                .filter(user -> user.getAccountStatus()
                        == AppUser.AccountStatus.ACTIVE)
                .filter(user -> user.getRole() == AppUser.Role.ADMIN)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.FORBIDDEN,
                        "Administrator access is required."));

        AppUser target = actorUserId.equals(targetUserId)
                ? actor
                : userRepository.findByIdForUpdate(targetUserId)
                        .orElseThrow(() -> new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "User not found."));

        AppUser.Role oldRole = target.getRole();
        if (oldRole == requestedRole) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "The user already has the requested role.");
        }

        if (actorUserId.equals(targetUserId)
                && oldRole == AppUser.Role.ADMIN) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Administrators cannot demote themselves.");
        }

        if (oldRole == AppUser.Role.ADMIN
                && target.getAccountStatus() == AppUser.AccountStatus.ACTIVE
                && requestedRole != AppUser.Role.ADMIN
                && userRepository.countByRoleAndAccountStatus(
                        AppUser.Role.ADMIN,
                        AppUser.AccountStatus.ACTIVE) <= 1) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "The last active administrator cannot be demoted.");
        }

        target.changeRole(requestedRole);
        AppUser savedTarget = userRepository.saveAndFlush(target);
        auditEventRepository.insert(new AuditEvent(
                UUID.randomUUID(),
                actor.getUserId(),
                savedTarget.getUserId(),
                AuditEvent.EventType.ROLE_CHANGED,
                oldRole,
                requestedRole,
                Map.of()));

        return UserResponse.from(savedTarget);
    }

    @Transactional
    public void bootstrapFirstAdmin(UUID targetUserId, String reason) {
        if (targetUserId == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "A target user is required for administrator bootstrap.");
        }
        if (reason == null || reason.isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "A nonblank bootstrap reason is required.");
        }

        lockRoleChanges();
        if (userRepository.countByRole(AppUser.Role.ADMIN) > 0
                || auditEventRepository.adminBootstrapExists()) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Administrator bootstrap has already been used.");
        }

        AppUser target = userRepository.findByIdForUpdate(targetUserId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Bootstrap target user not found."));
        if (target.getAccountStatus() != AppUser.AccountStatus.ACTIVE) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Only an active user can become the first administrator.");
        }

        target.changeRole(AppUser.Role.ADMIN);
        AppUser savedTarget = userRepository.saveAndFlush(target);
        auditEventRepository.insert(new AuditEvent(
                UUID.randomUUID(),
                null,
                savedTarget.getUserId(),
                AuditEvent.EventType.ADMIN_BOOTSTRAP,
                null,
                AppUser.Role.ADMIN,
                Map.of("reason", reason.trim())));
    }

    private void lockRoleChanges() {
        jdbcTemplate.execute(
                "SELECT pg_advisory_xact_lock("
                        + ROLE_LOCK_NAMESPACE + ", " + ROLE_LOCK_KEY + ")");
    }
}
