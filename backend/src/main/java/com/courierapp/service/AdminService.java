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

    // Company settings
    CompanySettingsResponse getCompanySettings();
    CompanySettingsResponse updateCompanySettings(CompanySettingsRequest request);

    // Courier ways
    List<CourierWayResponse> listCourierWays();
    List<CourierWayResponse> listActiveCourierWays();
    CourierWayResponse createCourierWay(CourierWayRequest request);
    CourierWayResponse updateCourierWay(Long id, CourierWayRequest request);
    void deleteCourierWay(Long id);

    // Departments
    List<DepartmentResponse> listDepartments();
    List<DepartmentResponse> listActiveDepartments();
    DepartmentResponse createDepartment(DepartmentRequest request);
    DepartmentResponse updateDepartment(Long id, DepartmentRequest request);
    void deleteDepartment(Long id);

    // Package types
    List<PackageTypeResponse> listPackageTypes();
    List<PackageTypeResponse> listActivePackageTypes();
    PackageTypeResponse createPackageType(PackageTypeRequest request);
    PackageTypeResponse updatePackageType(Long id, PackageTypeRequest request);
    void deletePackageType(Long id);

    // Companies
    List<CompanyResponse> listCompanies();
    List<CompanyResponse> listActiveCompanies();
    CompanyResponse createCompany(CompanyRequest request);
    CompanyResponse updateCompany(Long id, CompanyRequest request);
    void deleteCompany(Long id);
}
