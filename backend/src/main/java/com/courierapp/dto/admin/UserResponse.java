package com.courierapp.dto.admin;

import java.time.Instant;
import java.util.List;

public record UserResponse(
        Long id,
        String username,
        String fullName,
        String email,
        String phone,
        boolean active,
        List<RoleSummary> roles,
        List<PermissionResponse> directPermissions,
        Instant createdAt,
        String createdBy,
        Instant updatedAt,
        String updatedBy
) {
    public record RoleSummary(Long id, String name) {
    }
}
