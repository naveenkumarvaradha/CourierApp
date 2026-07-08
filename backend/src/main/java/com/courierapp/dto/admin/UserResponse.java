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
        Long departmentId,
        String departmentName,
        Long companyId,
        String companyCode,
        String companyName,
        List<RoleSummary> roles,
        List<PermissionResponse> directPermissions,
        Instant inactiveAt,
        Instant createdAt,
        String createdBy,
        Instant updatedAt,
        String updatedBy
) {
    public record RoleSummary(Long id, String name) {}
}
