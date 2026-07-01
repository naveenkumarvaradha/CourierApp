package com.courierapp.dto.admin;

import jakarta.validation.constraints.*;

import java.util.Set;

public record UserCreateRequest(
        @NotBlank(message = "Username is required")
        @Size(min = 3, max = 60, message = "Username must be 3-60 characters")
        String username,

        @NotBlank(message = "Password is required")
        @Size(min = 6, max = 100, message = "Password must be 6-100 characters")
        String password,

        @NotBlank(message = "Full name is required")
        @Size(max = 150)
        String fullName,

        @NotBlank(message = "Email is required")
        @Email(message = "Email must be valid")
        String email,

        @Size(max = 30)
        String phone,

        boolean active,

        Set<Long> roleIds,

        Set<Long> directPermissionIds
) {
}
