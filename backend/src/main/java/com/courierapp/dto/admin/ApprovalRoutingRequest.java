package com.courierapp.dto.admin;

public record ApprovalRoutingRequest(
        Long roleId,
        Long userId,
        Long creatorRoleId,
        boolean active
) {
}
