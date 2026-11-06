package com.auditor.user;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.verify;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.NONE,
        properties = {
                "spring.main.web-application-type=none",
                "app.bootstrap.enabled=true",
                "app.bootstrap.target-user-id=00000000-0000-0000-0000-000000000001",
                "app.bootstrap.reason=Initial setup"
        })
class NonWebBootstrapStartupTest {

    private static final UUID TARGET_USER_ID = UUID.fromString(
            "00000000-0000-0000-0000-000000000001");

    @Autowired
    private ApplicationContext context;

    @MockitoBean
    private RoleManagementService roleManagementService;

    @Test
    void fullApplicationStartsInNonWebModeAndRunsBootstrap() {
        assertFalse(context.containsBean("securityFilterChain"));
        verify(roleManagementService).bootstrapFirstAdmin(
                TARGET_USER_ID,
                "Initial setup");
    }
}
