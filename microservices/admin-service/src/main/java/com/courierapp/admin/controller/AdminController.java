package com.courierapp.admin.controller;

import com.courierapp.admin.dto.PageResponse;
import com.courierapp.admin.dto.admin.*;
import com.courierapp.admin.dto.audit.AuditLogResponse;
import com.courierapp.admin.service.AdminService;
import com.courierapp.admin.service.AuditLogService;
import com.courierapp.admin.service.EmailService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
@Tag(name = "Admin")
public class AdminController {

    private final AdminService adminService;
    private final AuditLogService auditLogService;
    private final EmailService emailService;

    // ----- Permissions -----

    @GetMapping("/permissions")
    @PreAuthorize("hasAuthority('ADMIN_VIEW')")
    @Operation(summary = "List all available permissions")
    public List<PermissionResponse> listPermissions() {
        return adminService.listPermissions();
    }

    // ----- Roles -----

    @GetMapping("/roles")
    @PreAuthorize("hasAuthority('ADMIN_VIEW')")
    public PageResponse<RoleResponse> listRoles(@PageableDefault(size = 20, sort = "name") Pageable pageable) {
        return adminService.listRoles(pageable);
    }

    @GetMapping("/roles/{id}")
    @PreAuthorize("hasAuthority('ADMIN_VIEW')")
    public RoleResponse getRole(@PathVariable Long id) {
        return adminService.getRole(id);
    }

    @PostMapping("/roles")
    @PreAuthorize("hasAuthority('ADMIN_CREATE')")
    @ResponseStatus(HttpStatus.CREATED)
    public RoleResponse createRole(@Valid @RequestBody RoleRequest request) {
        return adminService.createRole(request);
    }

    @PutMapping("/roles/{id}")
    @PreAuthorize("hasAuthority('ADMIN_UPDATE')")
    public RoleResponse updateRole(@PathVariable Long id, @Valid @RequestBody RoleRequest request) {
        return adminService.updateRole(id, request);
    }

    @DeleteMapping("/roles/{id}")
    @PreAuthorize("hasAuthority('ADMIN_DELETE')")
    public ResponseEntity<Void> deleteRole(@PathVariable Long id) {
        adminService.deleteRole(id);
        return ResponseEntity.noContent().build();
    }

    // ----- Users -----

    @GetMapping("/users")
    @PreAuthorize("hasAuthority('ADMIN_VIEW')")
    public PageResponse<UserResponse> listUsers(@RequestParam(required = false) String search,
                                                @PageableDefault(size = 20, sort = "username") Pageable pageable) {
        return adminService.listUsers(search, pageable);
    }

    @GetMapping("/users/{id}")
    @PreAuthorize("hasAuthority('ADMIN_VIEW')")
    public UserResponse getUser(@PathVariable Long id) {
        return adminService.getUser(id);
    }

    @PostMapping("/users")
    @PreAuthorize("hasAuthority('ADMIN_CREATE')")
    @ResponseStatus(HttpStatus.CREATED)
    public UserResponse createUser(@Valid @RequestBody UserCreateRequest request) {
        log.info("POST /admin/users username={}", request.username());
        return adminService.createUser(request);
    }

    @PutMapping("/users/{id}")
    @PreAuthorize("hasAuthority('ADMIN_UPDATE')")
    public UserResponse updateUser(@PathVariable Long id, @Valid @RequestBody UserUpdateRequest request) {
        return adminService.updateUser(id, request);
    }

    @DeleteMapping("/users/{id}")
    @PreAuthorize("hasAuthority('ADMIN_DELETE')")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
        adminService.deleteUser(id);
        return ResponseEntity.noContent().build();
    }

    // ----- MFA Admin Management -----

    @GetMapping("/users/mfa-status")
    @PreAuthorize("hasAuthority('ADMIN_VIEW')")
    @Operation(summary = "List all users with their MFA status")
    public PageResponse<UserMfaStatusResponse> listUserMfaStatus(
            @RequestParam(required = false) String search,
            @PageableDefault(size = 20, sort = "username") Pageable pageable) {
        return adminService.listUserMfaStatus(search, pageable);
    }

    @PostMapping("/users/{id}/mfa/disable")
    @PreAuthorize("hasAuthority('ADMIN_UPDATE')")
    @Operation(summary = "Disable MFA for a specific user (keeps secret)")
    public ResponseEntity<Void> adminDisableMfa(@PathVariable Long id) {
        adminService.adminDisableMfa(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/users/{id}/mfa/reset")
    @PreAuthorize("hasAuthority('ADMIN_UPDATE')")
    @Operation(summary = "Reset MFA for a specific user (disable + clear secret)")
    public ResponseEntity<Void> adminResetMfa(@PathVariable Long id) {
        adminService.adminResetMfa(id);
        return ResponseEntity.noContent().build();
    }

    // ----- User audit history -----

    @GetMapping("/users/{id}/history")
    @PreAuthorize("hasAuthority('ADMIN_VIEW')")
    @Operation(summary = "Get modification history for a specific user")
    public PageResponse<AuditLogResponse> getUserHistory(
            @PathVariable Long id,
            @PageableDefault(size = 20) Pageable pageable) {
        return auditLogService.getEntityHistory("USER", id, pageable);
    }

    // ----- Approval routing -----

    @GetMapping("/approval-routing")
    @PreAuthorize("hasAuthority('ADMIN_VIEW')")
    public List<ApprovalRoutingResponse> listApprovalRouting() {
        return adminService.listApprovalRouting();
    }

    @PostMapping("/approval-routing")
    @PreAuthorize("hasAuthority('ADMIN_CREATE')")
    @ResponseStatus(HttpStatus.CREATED)
    public ApprovalRoutingResponse createApprovalRouting(@Valid @RequestBody ApprovalRoutingRequest request) {
        log.info("POST /admin/approval-routing module={} roleId={} userId={}", request.module(), request.roleId(), request.userId());
        return adminService.createApprovalRouting(request);
    }

    @PutMapping("/approval-routing/{id}")
    @PreAuthorize("hasAuthority('ADMIN_UPDATE')")
    public ApprovalRoutingResponse updateApprovalRouting(@PathVariable Long id,
                                                          @Valid @RequestBody ApprovalRoutingRequest request) {
        return adminService.updateApprovalRouting(id, request);
    }

    @DeleteMapping("/approval-routing/{id}")
    @PreAuthorize("hasAuthority('ADMIN_DELETE')")
    public ResponseEntity<Void> deleteApprovalRouting(@PathVariable Long id) {
        adminService.deleteApprovalRouting(id);
        return ResponseEntity.noContent().build();
    }

    // ----- Password policy -----

    @GetMapping("/password-policy")
    @PreAuthorize("hasAuthority('ADMIN_VIEW')")
    @Operation(summary = "Get the current password policy")
    public PasswordPolicyResponse getPasswordPolicy() {
        return adminService.getPasswordPolicy();
    }

    @PutMapping("/password-policy")
    @PreAuthorize("hasAuthority('ADMIN_UPDATE')")
    @Operation(summary = "Update the password policy")
    public PasswordPolicyResponse updatePasswordPolicy(@Valid @RequestBody PasswordPolicyRequest request) {
        return adminService.updatePasswordPolicy(request);
    }

    // ----- Company settings -----

    @GetMapping("/company-settings")
    @PreAuthorize("hasAuthority('ADMIN_VIEW') or hasAuthority('BOOKING_CREATE')")
    @Operation(summary = "Get company settings")
    public CompanySettingsResponse getCompanySettings() {
        return adminService.getCompanySettings();
    }

    @PutMapping("/company-settings")
    @PreAuthorize("hasAuthority('ADMIN_UPDATE')")
    @Operation(summary = "Update company settings and sync the sender party")
    public CompanySettingsResponse updateCompanySettings(@Valid @RequestBody CompanySettingsRequest request) {
        log.info("PUT /admin/company-settings name={}", request.companyName());
        return adminService.updateCompanySettings(request);
    }

    // ----- Mail configuration -----

    @GetMapping("/mail-config")
    @PreAuthorize("hasAuthority('ADMIN_VIEW')")
    @Operation(summary = "Get global mail (SMTP) configuration")
    public MailConfigResponse getMailConfig() {
        return adminService.getMailConfig();
    }

    @PutMapping("/mail-config")
    @PreAuthorize("hasAuthority('ADMIN_UPDATE')")
    @Operation(summary = "Save global mail (SMTP) configuration")
    public MailConfigResponse saveMailConfig(@RequestBody MailConfigRequest request) {
        return adminService.saveMailConfig(request);
    }

    @PostMapping("/mail-config/test")
    @PreAuthorize("hasAuthority('ADMIN_VIEW')")
    @Operation(summary = "Send a test email")
    public ResponseEntity<Map<String, String>> testMailConfig(@RequestBody Map<String, String> body) {
        String toEmail = body.getOrDefault("email", "").trim();
        if (toEmail.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("message", "Email address is required"));
        }
        String host     = body.get("smtpHost");
        String portStr  = body.get("smtpPort");
        String username = body.get("smtpUsername");
        String password = body.get("smtpPassword");
        String fromName = body.get("smtpFromName");
        String tlsStr   = body.get("smtpTls");
        try {
            if (host != null && !host.isBlank() && username != null && !username.isBlank()
                    && password != null && !password.isBlank()) {
                int port = portStr != null ? Integer.parseInt(portStr) : 587;
                boolean tls = !"false".equalsIgnoreCase(tlsStr);
                emailService.sendTestEmailWithConfig(toEmail, host, port, username, password, fromName, tls);
            } else {
                emailService.sendTestEmail(toEmail);
            }
            return ResponseEntity.ok(Map.of("message", "Test email sent successfully to " + toEmail));
        } catch (Exception e) {
            log.error("Test mail failed to {}: {}", toEmail, e.getMessage());
            String msg = e.getCause() != null ? e.getCause().getMessage() : e.getMessage();
            return ResponseEntity.status(500).body(Map.of("message", "SMTP Error: " + msg));
        }
    }

    // ----- Courier ways -----

    @GetMapping("/courier-ways")
    @PreAuthorize("hasAuthority('ADMIN_VIEW')")
    public List<CourierWayResponse> listCourierWays() {
        return adminService.listCourierWays();
    }

    @GetMapping("/courier-ways/active")
    @PreAuthorize("hasAuthority('ADMIN_VIEW') or hasAuthority('BOOKING_CREATE') or hasAuthority('BOOKING_UPDATE')")
    public List<CourierWayResponse> listActiveCourierWays() {
        return adminService.listActiveCourierWays();
    }

    @PostMapping("/courier-ways")
    @PreAuthorize("hasAuthority('ADMIN_CREATE')")
    @ResponseStatus(HttpStatus.CREATED)
    public CourierWayResponse createCourierWay(@Valid @RequestBody CourierWayRequest request) {
        return adminService.createCourierWay(request);
    }

    @PutMapping("/courier-ways/{id}")
    @PreAuthorize("hasAuthority('ADMIN_UPDATE')")
    public CourierWayResponse updateCourierWay(@PathVariable Long id, @Valid @RequestBody CourierWayRequest request) {
        return adminService.updateCourierWay(id, request);
    }

    @DeleteMapping("/courier-ways/{id}")
    @PreAuthorize("hasAuthority('ADMIN_DELETE')")
    public ResponseEntity<Void> deleteCourierWay(@PathVariable Long id) {
        adminService.deleteCourierWay(id);
        return ResponseEntity.noContent().build();
    }

    // ----- Package types -----

    @GetMapping("/package-types")
    @PreAuthorize("hasAuthority('ADMIN_VIEW')")
    public List<PackageTypeResponse> listPackageTypes() {
        return adminService.listPackageTypes();
    }

    @GetMapping("/package-types/active")
    @PreAuthorize("hasAuthority('ADMIN_VIEW') or hasAuthority('BOOKING_CREATE') or hasAuthority('BOOKING_UPDATE')")
    public List<PackageTypeResponse> listActivePackageTypes() {
        return adminService.listActivePackageTypes();
    }

    @PostMapping("/package-types")
    @PreAuthorize("hasAuthority('ADMIN_CREATE')")
    @ResponseStatus(HttpStatus.CREATED)
    public PackageTypeResponse createPackageType(@Valid @RequestBody PackageTypeRequest request) {
        return adminService.createPackageType(request);
    }

    @PutMapping("/package-types/{id}")
    @PreAuthorize("hasAuthority('ADMIN_UPDATE')")
    public PackageTypeResponse updatePackageType(@PathVariable Long id, @Valid @RequestBody PackageTypeRequest request) {
        return adminService.updatePackageType(id, request);
    }

    @DeleteMapping("/package-types/{id}")
    @PreAuthorize("hasAuthority('ADMIN_DELETE')")
    public ResponseEntity<Void> deletePackageType(@PathVariable Long id) {
        adminService.deletePackageType(id);
        return ResponseEntity.noContent().build();
    }

    // ----- Departments -----

    @GetMapping("/departments")
    @PreAuthorize("hasAuthority('ADMIN_VIEW')")
    public List<DepartmentResponse> listDepartments() {
        return adminService.listDepartments();
    }

    @GetMapping("/departments/active")
    @PreAuthorize("isAuthenticated()")
    public List<DepartmentResponse> listActiveDepartments() {
        return adminService.listActiveDepartments();
    }

    @PostMapping("/departments")
    @PreAuthorize("hasAuthority('ADMIN_CREATE')")
    @ResponseStatus(HttpStatus.CREATED)
    public DepartmentResponse createDepartment(@Valid @RequestBody DepartmentRequest request) {
        return adminService.createDepartment(request);
    }

    @PutMapping("/departments/{id}")
    @PreAuthorize("hasAuthority('ADMIN_UPDATE')")
    public DepartmentResponse updateDepartment(@PathVariable Long id, @Valid @RequestBody DepartmentRequest request) {
        return adminService.updateDepartment(id, request);
    }

    @DeleteMapping("/departments/{id}")
    @PreAuthorize("hasAuthority('ADMIN_DELETE')")
    public ResponseEntity<Void> deleteDepartment(@PathVariable Long id) {
        adminService.deleteDepartment(id);
        return ResponseEntity.noContent().build();
    }

    // ----- Companies -----

    @GetMapping("/companies")
    @PreAuthorize("hasAuthority('ADMIN_VIEW')")
    public List<CompanyResponse> listCompanies() {
        return adminService.listCompanies();
    }

    @GetMapping("/companies/active")
    @PreAuthorize("hasAuthority('ADMIN_VIEW')")
    public List<CompanyResponse> listActiveCompanies() {
        return adminService.listActiveCompanies();
    }

    @PostMapping("/companies")
    @PreAuthorize("hasAuthority('ADMIN_CREATE')")
    @ResponseStatus(HttpStatus.CREATED)
    public CompanyResponse createCompany(@Valid @RequestBody CompanyRequest request) {
        return adminService.createCompany(request);
    }

    @PutMapping("/companies/{id}")
    @PreAuthorize("hasAuthority('ADMIN_UPDATE')")
    public CompanyResponse updateCompany(@PathVariable Long id, @Valid @RequestBody CompanyRequest request) {
        return adminService.updateCompany(id, request);
    }

    @DeleteMapping("/companies/{id}")
    @PreAuthorize("hasAuthority('ADMIN_DELETE')")
    public ResponseEntity<Void> deleteCompany(@PathVariable Long id) {
        adminService.deleteCompany(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/companies/{id}/settings")
    @PreAuthorize("hasAuthority('ADMIN_VIEW')")
    public CompanySettingsResponse getCompanySettings(@PathVariable Long id) {
        return adminService.getCompanySettingsByCompanyId(id);
    }

    @PutMapping("/companies/{id}/settings")
    @PreAuthorize("hasAuthority('ADMIN_UPDATE')")
    public CompanySettingsResponse updateCompanySettings(@PathVariable Long id,
                                                          @Valid @RequestBody CompanySettingsRequest request) {
        return adminService.updateCompanySettingsByCompanyId(id, request);
    }

    @PostMapping(value = "/companies/{id}/logo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAuthority('ADMIN_UPDATE')")
    @Operation(summary = "Upload or replace the company logo")
    public ResponseEntity<Map<String, String>> uploadLogo(
            @PathVariable Long id,
            @RequestParam("file") MultipartFile file) {
        try {
            adminService.saveCompanyLogo(id, file.getBytes(), file.getContentType());
            return ResponseEntity.ok(Map.of("message", "Logo uploaded successfully"));
        } catch (Exception e) {
            log.error("Logo upload failed for company {}: {}", id, e.getMessage());
            return ResponseEntity.status(500).body(Map.of("message", e.getMessage()));
        }
    }

    @GetMapping("/companies/{id}/logo")
    @Operation(summary = "Download the company logo")
    public ResponseEntity<byte[]> getLogo(@PathVariable Long id) {
        return adminService.getCompanyLogo(id)
                .map(logo -> ResponseEntity.ok()
                        .contentType(MediaType.parseMediaType(logo.contentType() != null ? logo.contentType() : "image/png"))
                        .body(logo.data()))
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/companies/{id}/logo")
    @PreAuthorize("hasAuthority('ADMIN_UPDATE')")
    public ResponseEntity<Void> deleteLogo(@PathVariable Long id) {
        adminService.deleteCompanyLogo(id);
        return ResponseEntity.noContent().build();
    }

    // ----- Sticker field config -----

    @GetMapping("/companies/{companyId}/sticker-config")
    @PreAuthorize("hasAuthority('ADMIN_VIEW')")
    public List<StickerFieldDto> getStickerConfig(@PathVariable Long companyId) {
        return adminService.getStickerFieldConfig(companyId);
    }

    @PutMapping("/companies/{companyId}/sticker-config")
    @PreAuthorize("hasAuthority('ADMIN_UPDATE')")
    public List<StickerFieldDto> saveStickerConfig(@PathVariable Long companyId,
                                                    @RequestBody List<StickerFieldDto> fields) {
        return adminService.saveStickerFieldConfig(companyId, fields);
    }
}
