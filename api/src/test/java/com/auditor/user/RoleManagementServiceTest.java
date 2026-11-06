package com.auditor.user;

import com.auditor.audit.AuditEvent;
import com.auditor.audit.AuditEventRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RoleManagementServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private AuditEventRepository auditEventRepository;

    @Mock
    private JdbcTemplate jdbcTemplate;

    @InjectMocks
    private RoleManagementService roleManagementService;

    @Test
    void roleChangeUpdatesUserAndRecordsAuditAfterAcquiringLock() {
        AppUser actor = user(AppUser.Role.ADMIN);
        AppUser target = user(AppUser.Role.USER);
        UUID actorId = actor.getUserId();
        UUID targetId = target.getUserId();
        when(userRepository.findByIdForUpdate(actorId))
                .thenReturn(Optional.of(actor));
        when(userRepository.findByIdForUpdate(targetId))
                .thenReturn(Optional.of(target));
        when(userRepository.saveAndFlush(target)).thenReturn(target);

        var result = roleManagementService.changeRole(
                actorId,
                targetId,
                AppUser.Role.RESEARCHER);

        assertEquals(AppUser.Role.RESEARCHER, target.getRole());
        assertEquals("RESEARCHER", result.role());

        ArgumentCaptor<AuditEvent> auditCaptor =
                ArgumentCaptor.forClass(AuditEvent.class);
        verify(auditEventRepository).insert(auditCaptor.capture());
        AuditEvent audit = auditCaptor.getValue();
        assertEquals(actorId, audit.actorUserId());
        assertEquals(targetId, audit.targetUserId());
        assertEquals(AuditEvent.EventType.ROLE_CHANGED, audit.eventType());
        assertEquals(AppUser.Role.USER, audit.oldRole());
        assertEquals(AppUser.Role.RESEARCHER, audit.newRole());
        assertEquals(Map.of(), audit.metadata());

        InOrder order = inOrder(jdbcTemplate, userRepository, auditEventRepository);
        order.verify(jdbcTemplate).execute(
                "SELECT pg_advisory_xact_lock(4281668, 1)");
        order.verify(userRepository).findByIdForUpdate(actorId);
        order.verify(userRepository).findByIdForUpdate(targetId);
        order.verify(userRepository).saveAndFlush(target);
        order.verify(auditEventRepository).insert(any(AuditEvent.class));
    }

    @Test
    void firstAdminBootstrapUsesSharedLockAndWritesBootstrapAudit() {
        AppUser target = user(AppUser.Role.USER);
        UUID targetId = target.getUserId();
        when(userRepository.countByRole(AppUser.Role.ADMIN)).thenReturn(0L);
        when(userRepository.findByIdForUpdate(targetId))
                .thenReturn(Optional.of(target));
        when(userRepository.saveAndFlush(target)).thenReturn(target);

        roleManagementService.bootstrapFirstAdmin(
                targetId,
                " Initial production setup ");

        assertEquals(AppUser.Role.ADMIN, target.getRole());
        ArgumentCaptor<AuditEvent> auditCaptor =
                ArgumentCaptor.forClass(AuditEvent.class);
        verify(auditEventRepository).insert(auditCaptor.capture());
        AuditEvent audit = auditCaptor.getValue();
        assertNull(audit.actorUserId());
        assertEquals(targetId, audit.targetUserId());
        assertEquals(AuditEvent.EventType.ADMIN_BOOTSTRAP, audit.eventType());
        assertNull(audit.oldRole());
        assertEquals(AppUser.Role.ADMIN, audit.newRole());
        assertEquals(Map.of("reason", "Initial production setup"), audit.metadata());

        InOrder order = inOrder(jdbcTemplate, userRepository, auditEventRepository);
        order.verify(jdbcTemplate).execute(
                "SELECT pg_advisory_xact_lock(4281668, 1)");
        order.verify(userRepository).countByRole(AppUser.Role.ADMIN);
        order.verify(auditEventRepository).adminBootstrapExists();
        order.verify(userRepository).findByIdForUpdate(targetId);
        order.verify(userRepository).saveAndFlush(target);
        order.verify(auditEventRepository).insert(any(AuditEvent.class));
    }

    @Test
    void refusesFirstAdminBootstrapWhenAnAdministratorAlreadyExists() {
        when(userRepository.countByRole(AppUser.Role.ADMIN)).thenReturn(1L);

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> roleManagementService.bootstrapFirstAdmin(
                        UUID.randomUUID(), "Initial setup"));

        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
        verify(userRepository, never()).findByIdForUpdate(any(UUID.class));
        verify(userRepository, never()).saveAndFlush(any(AppUser.class));
        verify(auditEventRepository, never()).insert(any(AuditEvent.class));
    }

    @Test
    void rejectsBlankBootstrapReason() {
        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> roleManagementService.bootstrapFirstAdmin(
                        UUID.randomUUID(), "  "));

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        verify(jdbcTemplate, never()).execute(any(String.class));
    }

    @Test
    void rejectsNoOpRoleChange() {
        AppUser actor = user(AppUser.Role.ADMIN);
        AppUser target = user(AppUser.Role.USER);
        UUID actorId = actor.getUserId();
        UUID targetId = target.getUserId();
        when(userRepository.findByIdForUpdate(actorId))
                .thenReturn(Optional.of(actor));
        when(userRepository.findByIdForUpdate(targetId))
                .thenReturn(Optional.of(target));

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> roleManagementService.changeRole(
                        actorId, targetId, AppUser.Role.USER));

        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
        verify(userRepository, never()).saveAndFlush(any(AppUser.class));
        verify(auditEventRepository, never()).insert(any(AuditEvent.class));
    }

    @Test
    void rejectsActorWhoseCurrentDatabaseRoleIsNotAdministrator() {
        AppUser actor = user(AppUser.Role.RESEARCHER);
        UUID actorId = actor.getUserId();
        when(userRepository.findByIdForUpdate(actorId))
                .thenReturn(Optional.of(actor));

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> roleManagementService.changeRole(
                        actorId, UUID.randomUUID(), AppUser.Role.ADMIN));

        assertEquals(HttpStatus.FORBIDDEN, exception.getStatusCode());
        verify(userRepository, never()).saveAndFlush(any(AppUser.class));
        verify(auditEventRepository, never()).insert(any(AuditEvent.class));
    }

    @Test
    void rejectsAdministratorSelfDemotion() {
        AppUser actor = user(AppUser.Role.ADMIN);
        UUID actorId = actor.getUserId();
        when(userRepository.findByIdForUpdate(actorId))
                .thenReturn(Optional.of(actor));

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> roleManagementService.changeRole(
                        actorId, actorId, AppUser.Role.USER));

        assertEquals(HttpStatus.FORBIDDEN, exception.getStatusCode());
        verify(userRepository, never()).saveAndFlush(any(AppUser.class));
        verify(auditEventRepository, never()).insert(any(AuditEvent.class));
    }

    @Test
    void rejectsDemotionOfLastActiveAdministrator() {
        AppUser actor = user(AppUser.Role.ADMIN);
        AppUser target = user(AppUser.Role.ADMIN);
        UUID actorId = actor.getUserId();
        UUID targetId = target.getUserId();
        when(userRepository.findByIdForUpdate(actorId))
                .thenReturn(Optional.of(actor));
        when(userRepository.findByIdForUpdate(targetId))
                .thenReturn(Optional.of(target));
        when(userRepository.countByRoleAndAccountStatus(
                AppUser.Role.ADMIN,
                AppUser.AccountStatus.ACTIVE))
                .thenReturn(1L);

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> roleManagementService.changeRole(
                        actorId, targetId, AppUser.Role.RESEARCHER));

        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
        verify(userRepository, never()).saveAndFlush(any(AppUser.class));
        verify(auditEventRepository, never()).insert(any(AuditEvent.class));
    }

    @Test
    void allowsDemotionWhenAnotherActiveAdministratorRemains() {
        AppUser actor = user(AppUser.Role.ADMIN);
        AppUser target = user(AppUser.Role.ADMIN);
        UUID actorId = actor.getUserId();
        UUID targetId = target.getUserId();
        when(userRepository.findByIdForUpdate(actorId))
                .thenReturn(Optional.of(actor));
        when(userRepository.findByIdForUpdate(targetId))
                .thenReturn(Optional.of(target));
        when(userRepository.countByRoleAndAccountStatus(
                AppUser.Role.ADMIN,
                AppUser.AccountStatus.ACTIVE))
                .thenReturn(2L);
        when(userRepository.saveAndFlush(target)).thenReturn(target);

        roleManagementService.changeRole(
                actorId, targetId, AppUser.Role.RESEARCHER);

        assertEquals(AppUser.Role.RESEARCHER, target.getRole());
        verify(auditEventRepository).insert(any(AuditEvent.class));
    }

    private AppUser user(AppUser.Role role) {
        AppUser user = new AppUser(
                UUID.randomUUID() + "@example.com",
                "password-hash",
                "Test User");
        if (role != AppUser.Role.USER) {
            user.changeRole(role);
        }
        return user;
    }
}
