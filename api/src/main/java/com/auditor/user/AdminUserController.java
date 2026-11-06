package com.auditor.user;

import com.auditor.auth.dto.UserResponse;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/users")
public class AdminUserController {

    private final RoleManagementService roleManagementService;

    public AdminUserController(RoleManagementService roleManagementService) {
        this.roleManagementService = roleManagementService;
    }

    @PatchMapping("/{userId}/role")
    public UserResponse changeRole(
            @PathVariable UUID userId,
            @Valid @RequestBody RoleChangeRequest request,
            Authentication authentication) {
        return roleManagementService.changeRole(
                UUID.fromString(authentication.getName()),
                userId,
                request.role());
    }
}
