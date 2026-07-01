package com.courierapp.dto.admin;

public record ApprovalRoutingResponse(
        Long id,
        Long roleId,
        String roleName,
        Long userId,
        String username,
        boolean active
) {
}
