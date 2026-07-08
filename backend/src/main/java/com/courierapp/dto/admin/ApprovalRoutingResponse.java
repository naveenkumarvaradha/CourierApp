package com.courierapp.dto.admin;

public record ApprovalRoutingResponse(
        Long id,
        Long roleId,
        String roleName,
        Long userId,
        String username,
        Long creatorRoleId,
        String creatorRoleName,
        Long creatorUserId,
        String creatorUsername,
        boolean active,
        String module,
        int level
) {
}
