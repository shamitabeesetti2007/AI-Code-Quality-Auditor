package com.auditor.user;

import jakarta.validation.constraints.NotNull;

public record RoleChangeRequest(@NotNull AppUser.Role role) {
}
