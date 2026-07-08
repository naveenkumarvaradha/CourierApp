package com.courierapp.admin.dto.admin;

import java.util.Set;

public record UserUpdateRequest(
        String fullName, String email, String phone, boolean active,
        String password, Set<Long> roleIds, Set<Long> directPermissionIds,
        Long departmentId, Long companyId
) {}
