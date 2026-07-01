package com.courierapp.controller;

import com.courierapp.dto.PageResponse;
import com.courierapp.dto.admin.*;
import com.courierapp.service.AdminService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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
}
