package com.courierapp.dto.admin;

import com.courierapp.enums.ActionType;
import com.courierapp.enums.ModuleType;

public record PermissionResponse(
        Long id,
        ModuleType module,
        ActionType action,
        String code,
        String description
) {
}
