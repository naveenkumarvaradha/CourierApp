package com.courierapp.dto.admin;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.Set;

public record RoleRequest(
        @NotBlank(message = "Role name is required")
        @Size(max = 60, message = "Role name must be at most 60 characters")
        String name,

        @Size(max = 255, message = "Description must be at most 255 characters")
        String description,

        Set<Long> permissionIds
) {
}
