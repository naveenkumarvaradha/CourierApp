package com.courierapp.controller;

import com.courierapp.dto.PageResponse;
import com.courierapp.dto.admin.*;
import com.courierapp.dto.admin.CompanySettingsRequest;
import com.courierapp.dto.admin.CompanySettingsResponse;
import com.courierapp.dto.admin.CourierWayRequest;
import com.courierapp.dto.admin.CourierWayResponse;
import com.courierapp.service.AdminService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/admin")
@Tag(name = "Admin")
public class AdminController {

    private final AdminService adminService;

    public AdminController(AdminService adminService) {
        this.adminService = adminService;
    }

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

    // ----- Company settings -----

    @GetMapping("/company-settings")
    @PreAuthorize("hasAuthority('ADMIN_VIEW') or hasAuthority('BOOKING_CREATE')")
    @Operation(summary = "Get company settings (used as sender on all bookings)")
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

    // ----- Courier ways -----

    @GetMapping("/courier-ways")
    @PreAuthorize("hasAuthority('ADMIN_VIEW')")
    @Operation(summary = "List all courier ways")
    public List<CourierWayResponse> listCourierWays() {
        return adminService.listCourierWays();
    }

    @GetMapping("/courier-ways/active")
    @PreAuthorize("hasAuthority('ADMIN_VIEW') or hasAuthority('BOOKING_CREATE') or hasAuthority('BOOKING_UPDATE')")
    @Operation(summary = "List active courier ways (for booking dropdowns)")
    public List<CourierWayResponse> listActiveCourierWays() {
        return adminService.listActiveCourierWays();
    }

    @PostMapping("/courier-ways")
    @PreAuthorize("hasAuthority('ADMIN_CREATE')")
    @ResponseStatus(HttpStatus.CREATED)
    public CourierWayResponse createCourierWay(@Valid @RequestBody CourierWayRequest request) {
        log.info("POST /admin/courier-ways name={}", request.name());
        return adminService.createCourierWay(request);
    }

    @PutMapping("/courier-ways/{id}")
    @PreAuthorize("hasAuthority('ADMIN_UPDATE')")
    public CourierWayResponse updateCourierWay(@PathVariable Long id,
                                               @Valid @RequestBody CourierWayRequest request) {
        log.info("PUT /admin/courier-ways/{}", id);
        return adminService.updateCourierWay(id, request);
    }

    @DeleteMapping("/courier-ways/{id}")
    @PreAuthorize("hasAuthority('ADMIN_DELETE')")
    public ResponseEntity<Void> deleteCourierWay(@PathVariable Long id) {
        log.info("DELETE /admin/courier-ways/{}", id);
        adminService.deleteCourierWay(id);
        return ResponseEntity.noContent().build();
    }

    // ----- Package types -----

    @GetMapping("/package-types")
    @PreAuthorize("hasAuthority('ADMIN_VIEW')")
    @Operation(summary = "List all package types")
    public List<PackageTypeResponse> listPackageTypes() {
        return adminService.listPackageTypes();
    }

    @GetMapping("/package-types/active")
    @PreAuthorize("hasAuthority('ADMIN_VIEW') or hasAuthority('BOOKING_CREATE') or hasAuthority('BOOKING_UPDATE')")
    @Operation(summary = "List active package types (for booking dropdowns)")
    public List<PackageTypeResponse> listActivePackageTypes() {
        return adminService.listActivePackageTypes();
    }

    @PostMapping("/package-types")
    @PreAuthorize("hasAuthority('ADMIN_CREATE')")
    @ResponseStatus(HttpStatus.CREATED)
    public PackageTypeResponse createPackageType(@Valid @RequestBody PackageTypeRequest request) {
        log.info("POST /admin/package-types name={}", request.name());
        return adminService.createPackageType(request);
    }

    @PutMapping("/package-types/{id}")
    @PreAuthorize("hasAuthority('ADMIN_UPDATE')")
    public PackageTypeResponse updatePackageType(@PathVariable Long id,
                                                 @Valid @RequestBody PackageTypeRequest request) {
        log.info("PUT /admin/package-types/{}", id);
        return adminService.updatePackageType(id, request);
    }

    @DeleteMapping("/package-types/{id}")
    @PreAuthorize("hasAuthority('ADMIN_DELETE')")
    public ResponseEntity<Void> deletePackageType(@PathVariable Long id) {
        log.info("DELETE /admin/package-types/{}", id);
        adminService.deletePackageType(id);
        return ResponseEntity.noContent().build();
    }

    // ----- Departments -----

    @GetMapping("/departments")
    @PreAuthorize("hasAuthority('ADMIN_VIEW')")
    @Operation(summary = "List all departments")
    public List<DepartmentResponse> listDepartments() {
        return adminService.listDepartments();
    }

    @GetMapping("/departments/active")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "List active departments (for user form dropdowns)")
    public List<DepartmentResponse> listActiveDepartments() {
        return adminService.listActiveDepartments();
    }

    @PostMapping("/departments")
    @PreAuthorize("hasAuthority('ADMIN_CREATE')")
    @ResponseStatus(HttpStatus.CREATED)
    public DepartmentResponse createDepartment(@Valid @RequestBody DepartmentRequest request) {
        log.info("POST /admin/departments name={}", request.name());
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
    @Operation(summary = "List all companies")
    public List<CompanyResponse> listCompanies() {
        return adminService.listCompanies();
    }

    @GetMapping("/companies/active")
    @PreAuthorize("hasAuthority('ADMIN_VIEW')")
    @Operation(summary = "List active companies")
    public List<CompanyResponse> listActiveCompanies() {
        return adminService.listActiveCompanies();
    }

    @PostMapping("/companies")
    @PreAuthorize("hasAuthority('ADMIN_CREATE')")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create a company")
    public CompanyResponse createCompany(@Valid @RequestBody CompanyRequest request) {
        return adminService.createCompany(request);
    }

    @PutMapping("/companies/{id}")
    @PreAuthorize("hasAuthority('ADMIN_UPDATE')")
    @Operation(summary = "Update a company")
    public CompanyResponse updateCompany(@PathVariable Long id, @Valid @RequestBody CompanyRequest request) {
        return adminService.updateCompany(id, request);
    }

    @DeleteMapping("/companies/{id}")
    @PreAuthorize("hasAuthority('ADMIN_DELETE')")
    public ResponseEntity<Void> deleteCompany(@PathVariable Long id) {
        adminService.deleteCompany(id);
        return ResponseEntity.noContent().build();
    }
}
