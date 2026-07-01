package com.courierapp.service.impl;

import com.courierapp.dto.PageResponse;
import com.courierapp.dto.admin.*;
import com.courierapp.entity.ApprovalRouting;
import com.courierapp.entity.Company;
import com.courierapp.entity.CompanySettings;
import com.courierapp.entity.CourierWay;
import com.courierapp.entity.Department;
import com.courierapp.entity.PackageType;
import com.courierapp.entity.Party;
import com.courierapp.entity.Permission;
import com.courierapp.entity.Role;
import com.courierapp.entity.User;
import com.courierapp.enums.PartyStatus;
import com.courierapp.enums.PartyType;
import com.courierapp.exception.BusinessException;
import com.courierapp.exception.DuplicateResourceException;
import com.courierapp.exception.ResourceNotFoundException;
import com.courierapp.mapper.PermissionMapper;
import com.courierapp.repository.ApprovalRoutingRepository;
import com.courierapp.repository.CompanyRepository;
import com.courierapp.repository.CompanySettingsRepository;
import com.courierapp.repository.CourierWayRepository;
import com.courierapp.repository.DepartmentRepository;
import com.courierapp.repository.PackageTypeRepository;
import com.courierapp.repository.PartyRepository;
import com.courierapp.repository.PermissionRepository;
import com.courierapp.repository.RoleRepository;
import com.courierapp.repository.UserRepository;
import com.courierapp.service.AdminService;
import com.courierapp.service.AuditLogService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@Transactional
public class AdminServiceImpl implements AdminService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;
    private final ApprovalRoutingRepository approvalRoutingRepository;
    private final CompanyRepository companyRepository;
    private final CompanySettingsRepository companySettingsRepository;
    private final CourierWayRepository courierWayRepository;
    private final PackageTypeRepository packageTypeRepository;
    private final DepartmentRepository departmentRepository;
    private final PartyRepository partyRepository;
    private final PasswordEncoder passwordEncoder;
    private final PermissionMapper permissionMapper;
    private final AuditLogService auditLogService;

    public AdminServiceImpl(UserRepository userRepository,
                            RoleRepository roleRepository,
                            PermissionRepository permissionRepository,
                            ApprovalRoutingRepository approvalRoutingRepository,
                            CompanyRepository companyRepository,
                            CompanySettingsRepository companySettingsRepository,
                            CourierWayRepository courierWayRepository,
                            PackageTypeRepository packageTypeRepository,
                            DepartmentRepository departmentRepository,
                            PartyRepository partyRepository,
                            PasswordEncoder passwordEncoder,
                            PermissionMapper permissionMapper,
                            AuditLogService auditLogService) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.permissionRepository = permissionRepository;
        this.approvalRoutingRepository = approvalRoutingRepository;
        this.companyRepository = companyRepository;
        this.companySettingsRepository = companySettingsRepository;
        this.courierWayRepository = courierWayRepository;
        this.packageTypeRepository = packageTypeRepository;
        this.departmentRepository = departmentRepository;
        this.partyRepository = partyRepository;
        this.passwordEncoder = passwordEncoder;
        this.permissionMapper = permissionMapper;
        this.auditLogService = auditLogService;
    }

    // ----- Permissions -----

    @Override
    @Transactional(readOnly = true)
    public List<PermissionResponse> listPermissions() {
        return permissionRepository.findAll().stream()
                .sorted(Comparator.comparing(Permission::getCode))
                .map(permissionMapper::toResponse)
                .toList();
    }

    // ----- Roles -----

    @Override
    @Transactional(readOnly = true)
    public PageResponse<RoleResponse> listRoles(Pageable pageable) {
        Page<Role> page = roleRepository.findAll(pageable);
        return PageResponse.from(page, this::toRoleResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public RoleResponse getRole(Long id) {
        return toRoleResponse(findRole(id));
    }

    @Override
    public RoleResponse createRole(RoleRequest request) {
        if (roleRepository.existsByName(request.name())) {
            throw new DuplicateResourceException("Role already exists: " + request.name());
        }
        Role role = new Role();
        role.setName(request.name());
        role.setDescription(request.description());
        role.setSystemRole(false);
        role.setPermissions(resolvePermissions(request.permissionIds()));
        Role saved = roleRepository.save(role);
        auditLogService.log("ROLE", "CREATE", saved.getId(), saved.getName(), "admin",
                "Permissions: " + request.permissionIds());
        return toRoleResponse(saved);
    }

    @Override
    public RoleResponse updateRole(Long id, RoleRequest request) {
        Role role = findRole(id);
        if (!role.getName().equals(request.name()) && roleRepository.existsByName(request.name())) {
            throw new DuplicateResourceException("Role already exists: " + request.name());
        }
        if (!role.isSystemRole()) {
            role.setName(request.name());
        }
        role.setDescription(request.description());
        role.setPermissions(resolvePermissions(request.permissionIds()));
        Role saved = roleRepository.save(role);
        auditLogService.log("ROLE", "UPDATE", saved.getId(), saved.getName(), "admin",
                "Permissions updated");
        return toRoleResponse(saved);
    }

    @Override
    public void deleteRole(Long id) {
        Role role = findRole(id);
        if (role.isSystemRole()) {
            throw new BusinessException("System roles cannot be deleted");
        }
        String name = role.getName();
        roleRepository.delete(role);
        auditLogService.log("ROLE", "DELETE", id, name, "admin", null);
    }

    // ----- Users -----

    @Override
    @Transactional(readOnly = true)
    public PageResponse<UserResponse> listUsers(String search, Pageable pageable) {
        Page<User> page;
        if (StringUtils.hasText(search)) {
            page = userRepository
                    .findByUsernameContainingIgnoreCaseOrFullNameContainingIgnoreCase(search, search, pageable);
        } else {
            page = userRepository.findAll(pageable);
        }
        return PageResponse.from(page, this::toUserResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponse getUser(Long id) {
        return toUserResponse(findUser(id));
    }

    @Override
    public UserResponse createUser(UserCreateRequest request) {
        if (userRepository.existsByUsername(request.username())) {
            throw new DuplicateResourceException("Username already exists: " + request.username());
        }
        if (userRepository.existsByEmail(request.email())) {
            throw new DuplicateResourceException("Email already exists: " + request.email());
        }
        User user = new User();
        user.setUsername(request.username());
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setFullName(request.fullName());
        user.setEmail(request.email());
        user.setPhone(request.phone());
        user.setActive(request.active());
        user.setRoles(resolveRoles(request.roleIds()));
        user.setDirectPermissions(resolvePermissions(request.directPermissionIds()));
        user.setDepartment(resolveDepartment(request.departmentId()));
        user.setCompany(resolveCompany(request.companyId()));
        User saved = userRepository.save(user);
        auditLogService.log("USER", "CREATE", saved.getId(), saved.getUsername(), "admin",
                "Email=" + saved.getEmail() + ", department=" + (saved.getDepartment() != null ? saved.getDepartment().getName() : "none"));
        return toUserResponse(saved);
    }

    @Override
    public UserResponse updateUser(Long id, UserUpdateRequest request) {
        User user = findUser(id);
        if (!user.getEmail().equals(request.email()) && userRepository.existsByEmail(request.email())) {
            throw new DuplicateResourceException("Email already exists: " + request.email());
        }
        user.setFullName(request.fullName());
        user.setEmail(request.email());
        user.setPhone(request.phone());
        user.setActive(request.active());
        if (StringUtils.hasText(request.password())) {
            user.setPasswordHash(passwordEncoder.encode(request.password()));
            auditLogService.log("USER", "PASSWORD_CHANGE", user.getId(), user.getUsername(), "admin", null);
        }
        user.setRoles(resolveRoles(request.roleIds()));
        user.setDirectPermissions(resolvePermissions(request.directPermissionIds()));
        user.setDepartment(resolveDepartment(request.departmentId()));
        user.setCompany(resolveCompany(request.companyId()));
        User saved = userRepository.save(user);
        auditLogService.log("USER", "UPDATE", saved.getId(), saved.getUsername(), "admin",
                "active=" + saved.isActive() + ", department=" + (saved.getDepartment() != null ? saved.getDepartment().getName() : "none"));
        return toUserResponse(saved);
    }

    @Override
    public void deleteUser(Long id) {
        User user = findUser(id);
        if ("admin".equalsIgnoreCase(user.getUsername())) {
            throw new BusinessException("The default admin user cannot be deleted");
        }
        String username = user.getUsername();
        userRepository.delete(user);
        auditLogService.log("USER", "DELETE", id, username, "admin", null);
    }

    // ----- Approval routing -----

    @Override
    @Transactional(readOnly = true)
    public List<ApprovalRoutingResponse> listApprovalRouting() {
        return approvalRoutingRepository.findAll().stream().map(this::toRoutingResponse).toList();
    }

    @Override
    public ApprovalRoutingResponse createApprovalRouting(ApprovalRoutingRequest request) {
        validateRouting(request);
        ApprovalRouting routing = new ApprovalRouting();
        applyRouting(routing, request);
        ApprovalRouting saved = approvalRoutingRepository.save(routing);
        auditLogService.log("APPROVAL_ROUTING", "CREATE", saved.getId(), "module=" + saved.getModule(), "admin",
                "role=" + (saved.getRole() != null ? saved.getRole().getName() : "") +
                ", user=" + (saved.getUser() != null ? saved.getUser().getUsername() : ""));
        return toRoutingResponse(saved);
    }

    @Override
    public ApprovalRoutingResponse updateApprovalRouting(Long id, ApprovalRoutingRequest request) {
        validateRouting(request);
        ApprovalRouting routing = approvalRoutingRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Approval routing", id));
        applyRouting(routing, request);
        ApprovalRouting saved = approvalRoutingRepository.save(routing);
        auditLogService.log("APPROVAL_ROUTING", "UPDATE", saved.getId(), "module=" + saved.getModule(), "admin", null);
        return toRoutingResponse(saved);
    }

    @Override
    public void deleteApprovalRouting(Long id) {
        ApprovalRouting routing = approvalRoutingRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Approval routing", id));
        approvalRoutingRepository.delete(routing);
        auditLogService.log("APPROVAL_ROUTING", "DELETE", id, "id=" + id, "admin", null);
    }

    private void validateRouting(ApprovalRoutingRequest request) {
        if (request.roleId() == null && request.userId() == null) {
            throw new BusinessException("Approval routing must reference either a role or a user");
        }
        if (request.roleId() != null && request.userId() != null) {
            throw new BusinessException("Approval routing must reference exactly one of role or user");
        }
    }

    private void applyRouting(ApprovalRouting routing, ApprovalRoutingRequest request) {
        if (request.roleId() != null) {
            routing.setRole(findRole(request.roleId()));
            routing.setUser(null);
        } else {
            routing.setUser(findUser(request.userId()));
            routing.setRole(null);
        }
        routing.setCreatorRole(request.creatorRoleId() != null ? findRole(request.creatorRoleId()) : null);
        routing.setCreatorUser(request.creatorUserId() != null ? findUser(request.creatorUserId()) : null);
        routing.setActive(request.active());
        routing.setModule(request.module() != null ? request.module().toUpperCase() : "BOOKING");
    }

    // ----- Helpers -----

    private Role findRole(Long id) {
        return roleRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Role", id));
    }

    private User findUser(Long id) {
        return userRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("User", id));
    }

    private Set<Permission> resolvePermissions(Set<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return new HashSet<>();
        }
        Set<Permission> result = new HashSet<>(permissionRepository.findAllById(ids));
        if (result.size() != ids.size()) {
            throw new BusinessException("One or more permission ids are invalid");
        }
        return result;
    }

    private Set<Role> resolveRoles(Set<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return new HashSet<>();
        }
        Set<Role> result = new HashSet<>(roleRepository.findAllById(ids));
        if (result.size() != ids.size()) {
            throw new BusinessException("One or more role ids are invalid");
        }
        return result;
    }

    private RoleResponse toRoleResponse(Role role) {
        List<PermissionResponse> perms = role.getPermissions().stream()
                .sorted(Comparator.comparing(Permission::getCode))
                .map(permissionMapper::toResponse)
                .toList();
        return new RoleResponse(role.getId(), role.getName(), role.getDescription(),
                role.isSystemRole(), perms);
    }

    private UserResponse toUserResponse(User user) {
        List<UserResponse.RoleSummary> roles = user.getRoles().stream()
                .sorted(Comparator.comparing(Role::getName))
                .map(r -> new UserResponse.RoleSummary(r.getId(), r.getName()))
                .toList();
        List<PermissionResponse> directPerms = user.getDirectPermissions().stream()
                .sorted(Comparator.comparing(Permission::getCode))
                .map(permissionMapper::toResponse)
                .toList();
        Long deptId = user.getDepartment() != null ? user.getDepartment().getId() : null;
        String deptName = user.getDepartment() != null ? user.getDepartment().getName() : null;
        Long companyId = user.getCompany() != null ? user.getCompany().getId() : null;
        String companyCode = user.getCompany() != null ? user.getCompany().getCompanyCode() : null;
        String companyName = user.getCompany() != null ? user.getCompany().getName() : null;
        return new UserResponse(user.getId(), user.getUsername(), user.getFullName(), user.getEmail(),
                user.getPhone(), user.isActive(), deptId, deptName, companyId, companyCode, companyName,
                roles, directPerms,
                user.getCreatedAt(), user.getCreatedBy(), user.getUpdatedAt(), user.getUpdatedBy());
    }

    private Department resolveDepartment(Long departmentId) {
        if (departmentId == null) return null;
        return departmentRepository.findById(departmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Department", departmentId));
    }

    private Company resolveCompany(Long companyId) {
        if (companyId == null) return null;
        return companyRepository.findById(companyId)
                .orElseThrow(() -> new ResourceNotFoundException("Company", companyId));
    }

    private ApprovalRoutingResponse toRoutingResponse(ApprovalRouting r) {
        return new ApprovalRoutingResponse(
                r.getId(),
                r.getRole() != null ? r.getRole().getId() : null,
                r.getRole() != null ? r.getRole().getName() : null,
                r.getUser() != null ? r.getUser().getId() : null,
                r.getUser() != null ? r.getUser().getUsername() : null,
                r.getCreatorRole() != null ? r.getCreatorRole().getId() : null,
                r.getCreatorRole() != null ? r.getCreatorRole().getName() : null,
                r.getCreatorUser() != null ? r.getCreatorUser().getId() : null,
                r.getCreatorUser() != null ? r.getCreatorUser().getUsername() : null,
                r.isActive(),
                r.getModule());
    }

    // ----- Company settings -----

    @Override
    @Transactional(readOnly = true)
    public CompanySettingsResponse getCompanySettings() {
        CompanySettings s = companySettingsRepository.findAll().stream().findFirst()
                .orElseThrow(() -> new BusinessException("Company settings not found"));
        return toSettingsResponse(s);
    }

    @Override
    public CompanySettingsResponse updateCompanySettings(CompanySettingsRequest req) {
        log.info("Updating company settings: name={}", req.companyName());
        CompanySettings s = companySettingsRepository.findAll().stream().findFirst()
                .orElse(new CompanySettings());
        s.setCompanyName(req.companyName());
        s.setAddressLine1(req.addressLine1());
        s.setAddressLine2(req.addressLine2());
        s.setCity(req.city());
        s.setState(req.state());
        s.setPincode(req.pincode());
        s.setCountry(req.country());
        s.setPhone(req.phone());
        s.setEmail(req.email());
        s.setGstin(req.gstin());

        // Upsert the linked company party so bookings always have a sender
        Party party = s.getLinkedParty() != null
                ? s.getLinkedParty()
                : partyRepository.findByPartyCode("COMPANY001").orElse(new Party());
        party.setPartyCode("COMPANY001");
        party.setPartyName(req.companyName());
        party.setAddressLine1(req.addressLine1());
        party.setAddressLine2(req.addressLine2());
        party.setCity(req.city());
        party.setState(req.state());
        party.setPincode(req.pincode());
        party.setCountry(req.country());
        party.setPhone(req.phone());
        party.setEmail(req.email());
        party.setGstin(req.gstin());
        party.setPartyType(PartyType.SENDER);
        party.setActive(true);
        party.setPartyStatus(PartyStatus.ACTIVE);
        Party savedParty = partyRepository.save(party);

        s.setLinkedParty(savedParty);
        CompanySettings saved = companySettingsRepository.save(s);
        log.info("Company settings saved id={}, linked party id={}", saved.getId(), savedParty.getId());
        auditLogService.log("COMPANY", "UPDATE", saved.getId(), req.companyName(), "admin",
                "Address: " + req.city() + ", " + req.pincode());
        return toSettingsResponse(saved);
    }

    private CompanySettingsResponse toSettingsResponse(CompanySettings s) {
        return new CompanySettingsResponse(s.getId(), s.getCompanyName(), s.getAddressLine1(),
                s.getAddressLine2(), s.getCity(), s.getState(), s.getPincode(),
                s.getCountry(), s.getPhone(), s.getEmail(), s.getGstin());
    }

    // ----- Courier ways -----

    @Override
    @Transactional(readOnly = true)
    public List<CourierWayResponse> listCourierWays() {
        return courierWayRepository.findAll().stream()
                .map(this::toCourierWayResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<CourierWayResponse> listActiveCourierWays() {
        return courierWayRepository.findByActiveTrueOrderByNameAsc().stream()
                .map(this::toCourierWayResponse).toList();
    }

    @Override
    public CourierWayResponse createCourierWay(CourierWayRequest req) {
        log.info("Creating courier way: name={}", req.name());
        if (courierWayRepository.existsByNameIgnoreCase(req.name())) {
            throw new BusinessException("Courier way '" + req.name() + "' already exists");
        }
        CourierWay cw = new CourierWay();
        cw.setName(req.name().toUpperCase());
        cw.setActive(req.active());
        CourierWay saved = courierWayRepository.save(cw);
        log.info("Courier way created: id={}, name={}", saved.getId(), saved.getName());
        auditLogService.log("COURIER_WAY", "CREATE", saved.getId(), saved.getName(), "admin", null);
        return toCourierWayResponse(saved);
    }

    @Override
    public CourierWayResponse updateCourierWay(Long id, CourierWayRequest req) {
        log.info("Updating courier way id={}: name={}", id, req.name());
        CourierWay cw = courierWayRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Courier way", id));
        if (!cw.getName().equalsIgnoreCase(req.name())
                && courierWayRepository.existsByNameIgnoreCase(req.name())) {
            throw new BusinessException("Courier way '" + req.name() + "' already exists");
        }
        cw.setName(req.name().toUpperCase());
        cw.setActive(req.active());
        CourierWay saved = courierWayRepository.save(cw);
        auditLogService.log("COURIER_WAY", "UPDATE", saved.getId(), saved.getName(), "admin",
                "active=" + saved.isActive());
        return toCourierWayResponse(saved);
    }

    @Override
    public void deleteCourierWay(Long id) {
        log.info("Deleting courier way id={}", id);
        CourierWay cw = courierWayRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Courier way", id));
        String name = cw.getName();
        courierWayRepository.delete(cw);
        auditLogService.log("COURIER_WAY", "DELETE", id, name, "admin", null);
    }

    private CourierWayResponse toCourierWayResponse(CourierWay cw) {
        return new CourierWayResponse(cw.getId(), cw.getName(), cw.isActive());
    }

    // ----- Package types -----

    @Override
    @Transactional(readOnly = true)
    public List<PackageTypeResponse> listPackageTypes() {
        return packageTypeRepository.findAll().stream()
                .map(this::toPackageTypeResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<PackageTypeResponse> listActivePackageTypes() {
        return packageTypeRepository.findByActiveTrueOrderByNameAsc().stream()
                .map(this::toPackageTypeResponse).toList();
    }

    @Override
    public PackageTypeResponse createPackageType(PackageTypeRequest req) {
        log.info("Creating package type: name={}", req.name());
        if (packageTypeRepository.existsByNameIgnoreCase(req.name())) {
            throw new BusinessException("Package type '" + req.name() + "' already exists");
        }
        PackageType pt = new PackageType();
        pt.setName(req.name().toUpperCase());
        pt.setActive(req.active());
        PackageType saved = packageTypeRepository.save(pt);
        log.info("Package type created id={}", saved.getId());
        auditLogService.log("PACKAGE_TYPE", "CREATE", saved.getId(), saved.getName(), "admin", null);
        return toPackageTypeResponse(saved);
    }

    @Override
    public PackageTypeResponse updatePackageType(Long id, PackageTypeRequest req) {
        log.info("Updating package type id={}: name={}", id, req.name());
        PackageType pt = packageTypeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Package type", id));
        if (!pt.getName().equalsIgnoreCase(req.name())
                && packageTypeRepository.existsByNameIgnoreCase(req.name())) {
            throw new BusinessException("Package type '" + req.name() + "' already exists");
        }
        pt.setName(req.name().toUpperCase());
        pt.setActive(req.active());
        PackageType saved = packageTypeRepository.save(pt);
        auditLogService.log("PACKAGE_TYPE", "UPDATE", saved.getId(), saved.getName(), "admin",
                "active=" + saved.isActive());
        return toPackageTypeResponse(saved);
    }

    @Override
    public void deletePackageType(Long id) {
        log.info("Deleting package type id={}", id);
        PackageType pt = packageTypeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Package type", id));
        String name = pt.getName();
        packageTypeRepository.delete(pt);
        auditLogService.log("PACKAGE_TYPE", "DELETE", id, name, "admin", null);
    }

    private PackageTypeResponse toPackageTypeResponse(PackageType pt) {
        return new PackageTypeResponse(pt.getId(), pt.getName(), pt.isActive());
    }

    // ----- Departments -----

    @Override
    @Transactional(readOnly = true)
    public List<DepartmentResponse> listDepartments() {
        return departmentRepository.findAll().stream().map(this::toDeptResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<DepartmentResponse> listActiveDepartments() {
        return departmentRepository.findByActiveTrueOrderByNameAsc().stream().map(this::toDeptResponse).toList();
    }

    @Override
    public DepartmentResponse createDepartment(DepartmentRequest req) {
        log.info("Creating department: name={}", req.name());
        if (departmentRepository.existsByNameIgnoreCase(req.name())) {
            throw new BusinessException("Department '" + req.name() + "' already exists");
        }
        Department d = new Department();
        d.setName(req.name());
        d.setActive(req.active());
        Department saved = departmentRepository.save(d);
        auditLogService.log("DEPARTMENT", "CREATE", saved.getId(), saved.getName(), "admin", null);
        return toDeptResponse(saved);
    }

    @Override
    public DepartmentResponse updateDepartment(Long id, DepartmentRequest req) {
        log.info("Updating department id={}", id);
        Department d = departmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Department", id));
        if (!d.getName().equalsIgnoreCase(req.name()) && departmentRepository.existsByNameIgnoreCase(req.name())) {
            throw new BusinessException("Department '" + req.name() + "' already exists");
        }
        d.setName(req.name());
        d.setActive(req.active());
        Department saved = departmentRepository.save(d);
        auditLogService.log("DEPARTMENT", "UPDATE", saved.getId(), saved.getName(), "admin",
                "active=" + saved.isActive());
        return toDeptResponse(saved);
    }

    @Override
    public void deleteDepartment(Long id) {
        log.info("Deleting department id={}", id);
        Department d = departmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Department", id));
        String name = d.getName();
        departmentRepository.delete(d);
        auditLogService.log("DEPARTMENT", "DELETE", id, name, "admin", null);
    }

    private DepartmentResponse toDeptResponse(Department d) {
        return new DepartmentResponse(d.getId(), d.getName(), d.isActive());
    }

    // ----- Companies -----

    @Override
    @Transactional(readOnly = true)
    public List<CompanyResponse> listCompanies() {
        return companyRepository.findAll().stream().map(this::toCompanyResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<CompanyResponse> listActiveCompanies() {
        return companyRepository.findByActiveTrueOrderByNameAsc().stream().map(this::toCompanyResponse).toList();
    }

    @Override
    public CompanyResponse createCompany(CompanyRequest req) {
        log.info("Creating company: code={}, name={}", req.companyCode(), req.name());
        if (companyRepository.existsByCompanyCodeIgnoreCase(req.companyCode())) {
            throw new BusinessException("Company code '" + req.companyCode() + "' already exists");
        }
        if (companyRepository.existsByNameIgnoreCase(req.name())) {
            throw new BusinessException("Company name '" + req.name() + "' already exists");
        }
        Company c = Company.builder()
                .companyCode(req.companyCode().toUpperCase())
                .name(req.name())
                .active(req.active())
                .build();
        Company saved = companyRepository.save(c);
        auditLogService.log("COMPANY", "CREATE", saved.getId(), saved.getCompanyCode() + " " + saved.getName(), "admin", null);
        return toCompanyResponse(saved);
    }

    @Override
    public CompanyResponse updateCompany(Long id, CompanyRequest req) {
        log.info("Updating company id={}", id);
        Company c = companyRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Company", id));
        if (!c.getCompanyCode().equalsIgnoreCase(req.companyCode())
                && companyRepository.existsByCompanyCodeIgnoreCase(req.companyCode())) {
            throw new BusinessException("Company code '" + req.companyCode() + "' already exists");
        }
        if (!c.getName().equalsIgnoreCase(req.name())
                && companyRepository.existsByNameIgnoreCase(req.name())) {
            throw new BusinessException("Company name '" + req.name() + "' already exists");
        }
        c.setCompanyCode(req.companyCode().toUpperCase());
        c.setName(req.name());
        c.setActive(req.active());
        Company saved = companyRepository.save(c);
        auditLogService.log("COMPANY", "UPDATE", saved.getId(), saved.getCompanyCode() + " " + saved.getName(), "admin",
                "active=" + saved.isActive());
        return toCompanyResponse(saved);
    }

    @Override
    public void deleteCompany(Long id) {
        log.info("Deleting company id={}", id);
        Company c = companyRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Company", id));
        String name = c.getCompanyCode() + " " + c.getName();
        companyRepository.delete(c);
        auditLogService.log("COMPANY", "DELETE", id, name, "admin", null);
    }

    private CompanyResponse toCompanyResponse(Company c) {
        return new CompanyResponse(c.getId(), c.getCompanyCode(), c.getName(), c.isActive());
    }
}
