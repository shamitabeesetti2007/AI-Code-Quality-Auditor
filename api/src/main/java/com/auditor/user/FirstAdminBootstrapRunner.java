package com.auditor.user;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@ConditionalOnProperty(
        prefix = "app.bootstrap",
        name = "enabled",
        havingValue = "true")
public class FirstAdminBootstrapRunner implements ApplicationRunner {

    private static final Logger logger =
            LoggerFactory.getLogger(FirstAdminBootstrapRunner.class);

    private final RoleManagementService roleManagementService;
    private final String targetUserId;
    private final String reason;

    public FirstAdminBootstrapRunner(
            RoleManagementService roleManagementService,
            Environment environment) {
        if (!"none".equalsIgnoreCase(
                environment.getProperty("spring.main.web-application-type"))) {
            throw new IllegalStateException(
                    "Administrator bootstrap requires "
                            + "spring.main.web-application-type=none.");
        }
        this.roleManagementService = roleManagementService;
        this.targetUserId =
                environment.getProperty("app.bootstrap.target-user-id", "");
        this.reason = environment.getProperty("app.bootstrap.reason", "");
    }

    @Override
    public void run(ApplicationArguments args) {
        UUID userId;
        try {
            userId = UUID.fromString(targetUserId);
        } catch (IllegalArgumentException exception) {
            throw new IllegalStateException(
                    "A valid app.bootstrap.target-user-id is required.",
                    exception);
        }
        if (reason.isBlank()) {
            throw new IllegalStateException(
                    "A nonblank app.bootstrap.reason is required.");
        }

        roleManagementService.bootstrapFirstAdmin(userId, reason);
        logger.info("Bootstrapped the first administrator for user {}.", userId);
    }
}
