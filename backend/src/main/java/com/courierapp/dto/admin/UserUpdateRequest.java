package com.courierapp.dto.admin;

import jakarta.validation.constraints.*;

import java.util.Set;

public record UserUpdateRequest(
        @NotBlank(message = "Full name is required")
        @Size(max = 150)
        String fullName,

        @NotBlank(message = "Email is required")
        @Email(message = "Email must be valid")
        String email,

        @Size(max = 30)
        String phone,

        boolean active,

        /** Optional. If provided (non-blank), resets the password. */
        @Size(min = 6, max = 100, message = "Password must be 6-100 characters")
        String password,

        Set<Long> roleIds,

        Set<Long> directPermissionIds
) {
}
