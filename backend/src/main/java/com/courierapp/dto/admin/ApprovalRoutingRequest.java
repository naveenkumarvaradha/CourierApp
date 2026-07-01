package com.courierapp.dto.admin;

public record ApprovalRoutingRequest(
        Long roleId,
        Long userId,
        Long creatorRoleId,
        Long creatorUserId,
        boolean active,
        String module
) {
}
