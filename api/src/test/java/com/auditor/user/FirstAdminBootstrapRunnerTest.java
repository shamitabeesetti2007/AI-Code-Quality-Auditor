package com.auditor.user;

import org.junit.jupiter.api.Test;
import org.springframework.boot.ApplicationArguments;
import org.springframework.core.env.Environment;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class FirstAdminBootstrapRunnerTest {

    @Test
    void invokesBootstrapWithOperatorArgumentsInNonWebMode() {
        UUID targetUserId = UUID.randomUUID();
        String reason = "Initial production setup";
        Environment environment = mock(Environment.class);
        when(environment.getProperty("spring.main.web-application-type"))
                .thenReturn("none");
        when(environment.getProperty("app.bootstrap.target-user-id", ""))
                .thenReturn(targetUserId.toString());
        when(environment.getProperty("app.bootstrap.reason", ""))
                .thenReturn(reason);
        RoleManagementService roleManagementService =
                mock(RoleManagementService.class);

        FirstAdminBootstrapRunner runner = new FirstAdminBootstrapRunner(
                roleManagementService, environment);
        runner.run(mock(ApplicationArguments.class));

        verify(roleManagementService).bootstrapFirstAdmin(targetUserId, reason);
    }

    @Test
    void refusesToRunInWebApplicationMode() {
        Environment environment = mock(Environment.class);
        when(environment.getProperty("spring.main.web-application-type"))
                .thenReturn("servlet");

        assertThrows(
                IllegalStateException.class,
                () -> new FirstAdminBootstrapRunner(
                        mock(RoleManagementService.class),
                        environment));
    }

    @Test
    void rejectsMalformedTargetWithoutCallingBootstrapService() {
        Environment environment = mock(Environment.class);
        when(environment.getProperty("spring.main.web-application-type"))
                .thenReturn("none");
        when(environment.getProperty("app.bootstrap.target-user-id", ""))
                .thenReturn("not-a-user-id");
        when(environment.getProperty("app.bootstrap.reason", ""))
                .thenReturn("Initial setup");
        RoleManagementService roleManagementService =
                mock(RoleManagementService.class);

        FirstAdminBootstrapRunner runner = new FirstAdminBootstrapRunner(
                roleManagementService, environment);

        assertThrows(
                IllegalStateException.class,
                () -> runner.run(mock(ApplicationArguments.class)));
        verify(roleManagementService, never())
                .bootstrapFirstAdmin(any(UUID.class), any(String.class));
    }

    @Test
    void refusesToRunWhenWebApplicationModeIsNotExplicitlyDisabled() {
        Environment environment = mock(Environment.class);

        assertThrows(
                IllegalStateException.class,
                () -> new FirstAdminBootstrapRunner(
                        mock(RoleManagementService.class),
                        environment));
    }
}
