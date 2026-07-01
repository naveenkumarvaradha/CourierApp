package com.courierapp.service;

import com.courierapp.dto.PageResponse;
import com.courierapp.dto.admin.*;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface AdminService {

    // Permissions
    List<PermissionResponse> listPermissions();

    // Roles
    PageResponse<RoleResponse> listRoles(Pageable pageable);
    RoleResponse getRole(Long id);
    RoleResponse createRole(RoleRequest request);
    RoleResponse updateRole(Long id, RoleRequest request);
    void deleteRole(Long id);

    // Users
    PageResponse<UserResponse> listUsers(String search, Pageable pageable);
    UserResponse getUser(Long id);
    UserResponse createUser(UserCreateRequest request);
    UserResponse updateUser(Long id, UserUpdateRequest request);
    void deleteUser(Long id);

    // Approval routing
    List<ApprovalRoutingResponse> listApprovalRouting();
    ApprovalRoutingResponse createApprovalRouting(ApprovalRoutingRequest request);
    ApprovalRoutingResponse updateApprovalRouting(Long id, ApprovalRoutingRequest request);
    void deleteApprovalRouting(Long id);
}
