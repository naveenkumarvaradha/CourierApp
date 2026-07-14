package com.courierapp.admin.dto.admin;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.Set;

public record UserCreateRequest(
        @NotBlank @Size(max = 100) String username,
        @NotBlank @Size(min = 8, max = 100) String password,
        String fullName,
        @Email @Size(max = 255) String email,
        String phone, boolean active,
        Set<Long> roleIds, Set<Long> directPermissionIds, Long departmentId, Long companyId
) {}
