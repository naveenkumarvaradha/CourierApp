package com.courierapp.dto.admin;

public record ApprovalRoutingRequest(
        Long roleId,
        Long userId,
        boolean active
) {
}
