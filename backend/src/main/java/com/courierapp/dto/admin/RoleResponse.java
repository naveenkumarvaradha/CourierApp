package com.courierapp.dto.admin;

import java.util.List;

public record RoleResponse(
        Long id,
        String name,
        String description,
        boolean systemRole,
        List<PermissionResponse> permissions
) {
}
