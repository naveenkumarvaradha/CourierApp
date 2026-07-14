package com.courierapp.admin.dto.admin;

import jakarta.validation.constraints.NotBlank;
import java.util.Set;

public record UserCreateRequest(
        @NotBlank String username, @NotBlank String password,
        String fullName, String email, String phone, boolean active,
        Set<Long> roleIds, Set<Long> directPermissionIds, Long departmentId, Long companyId
) {}
