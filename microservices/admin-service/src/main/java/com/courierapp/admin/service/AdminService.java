package com.courierapp.admin.service;

import com.courierapp.admin.dto.PageResponse;
import com.courierapp.admin.dto.admin.*;

import java.util.List;
import java.util.Optional;

public interface AdminService {
    List<PermissionResponse> listPermissions();

    PageResponse<RoleResponse> listRoles(org.springframework.data.domain.Pageable pageable);
    RoleResponse getRole(Long id);
    RoleResponse createRole(RoleRequest request);
    RoleResponse updateRole(Long id, RoleRequest request);
    void deleteRole(Long id);

    PageResponse<UserResponse> listUsers(String search, org.springframework.data.domain.Pageable pageable);
    UserResponse getUser(Long id);
    UserResponse createUser(UserCreateRequest request);
    UserResponse updateUser(Long id, UserUpdateRequest request);
    void deleteUser(Long id);

    PageResponse<UserMfaStatusResponse> listUserMfaStatus(String search, org.springframework.data.domain.Pageable pageable);
    void adminDisableMfa(Long userId);
    void adminResetMfa(Long userId);

    List<ApprovalRoutingResponse> listApprovalRouting();
    ApprovalRoutingResponse createApprovalRouting(ApprovalRoutingRequest request);
    ApprovalRoutingResponse updateApprovalRouting(Long id, ApprovalRoutingRequest request);
    void deleteApprovalRouting(Long id);

    PasswordPolicyResponse getPasswordPolicy();
    PasswordPolicyResponse updatePasswordPolicy(PasswordPolicyRequest request);

    CompanySettingsResponse getCompanySettings();
    CompanySettingsResponse updateCompanySettings(CompanySettingsRequest request);
    CompanySettingsResponse getCompanySettingsByCompanyId(Long companyId);
    CompanySettingsResponse updateCompanySettingsByCompanyId(Long companyId, CompanySettingsRequest request);

    MailConfigResponse getMailConfig();
    MailConfigResponse saveMailConfig(MailConfigRequest request);

    void saveCompanyLogo(Long companyId, byte[] data, String contentType);
    Optional<LogoDto> getCompanyLogo(Long companyId);
    void deleteCompanyLogo(Long companyId);

    List<CourierWayResponse> listCourierWays();
    List<CourierWayResponse> listActiveCourierWays();
    CourierWayResponse createCourierWay(CourierWayRequest request);
    CourierWayResponse updateCourierWay(Long id, CourierWayRequest request);
    void deleteCourierWay(Long id);

    List<DepartmentResponse> listDepartments();
    List<DepartmentResponse> listActiveDepartments();
    DepartmentResponse createDepartment(DepartmentRequest request);
    DepartmentResponse updateDepartment(Long id, DepartmentRequest request);
    void deleteDepartment(Long id);

    List<PackageTypeResponse> listPackageTypes();
    List<PackageTypeResponse> listActivePackageTypes();
    PackageTypeResponse createPackageType(PackageTypeRequest request);
    PackageTypeResponse updatePackageType(Long id, PackageTypeRequest request);
    void deletePackageType(Long id);

    List<CompanyResponse> listCompanies();
    List<CompanyResponse> listActiveCompanies();
    CompanyResponse createCompany(CompanyRequest request);
    CompanyResponse updateCompany(Long id, CompanyRequest request);
    void deleteCompany(Long id);

    List<StickerFieldDto> getStickerFieldConfig(Long companyId);
    List<StickerFieldDto> saveStickerFieldConfig(Long companyId, List<StickerFieldDto> fields);
}
