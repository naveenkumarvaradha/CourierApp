package com.courierapp.admin.service.impl;

import com.courierapp.admin.dto.PageResponse;
import com.courierapp.admin.dto.admin.*;
import com.courierapp.admin.entity.*;
import com.courierapp.admin.exception.BusinessException;
import com.courierapp.admin.exception.ResourceNotFoundException;
import com.courierapp.admin.repository.*;
import com.courierapp.admin.service.AdminService;
import com.courierapp.admin.service.AuditLogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
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
    private final PasswordPolicyRepository passwordPolicyRepository;
    private final StickerFieldConfigRepository stickerFieldConfigRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditLogService auditLogService;

    // ----- Permissions -----

    @Override
    @Transactional(readOnly = true)
    public List<PermissionResponse> listPermissions() {
        return permissionRepository.findAll().stream()
                .sorted(Comparator.comparing(Permission::getCode))
                .map(p -> new PermissionResponse(p.getId(), p.getCode(), p.getDescription()))
                .toList();
    }

    // ----- Roles -----

    @Override
    @Transactional(readOnly = true)
    public PageResponse<RoleResponse> listRoles(Pageable pageable) {
        return PageResponse.from(roleRepository.findAll(pageable), this::toRoleResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public RoleResponse getRole(Long id) {
        return toRoleResponse(findRole(id));
    }

    @Override
    public RoleResponse createRole(RoleRequest request) {
        if (roleRepository.existsByName(request.name())) {
            throw new BusinessException("Role already exists: " + request.name());
        }
        Role role = new Role();
        role.setName(request.name());
        role.setDescription(request.description());
        role.setSystemRole(false);
        role.setPermissions(resolvePermissions(request.permissionIds()));
        Role saved = roleRepository.save(role);
        auditLogService.log("ROLE", "CREATE", saved.getId(), saved.getName(), currentUser(),
                "Permissions: " + request.permissionIds());
        return toRoleResponse(saved);
    }

    @Override
    public RoleResponse updateRole(Long id, RoleRequest request) {
        Role role = findRole(id);
        if (!role.getName().equals(request.name()) && roleRepository.existsByName(request.name())) {
            throw new BusinessException("Role already exists: " + request.name());
        }
        if (!role.isSystemRole()) {
            role.setName(request.name());
        }
        role.setDescription(request.description());
        role.setPermissions(resolvePermissions(request.permissionIds()));
        Role saved = roleRepository.save(role);
        auditLogService.log("ROLE", "UPDATE", saved.getId(), saved.getName(), currentUser(), "Permissions updated");
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
        auditLogService.log("ROLE", "DELETE", id, name, currentUser(), null);
    }

    // ----- Users -----

    @Override
    @Transactional(readOnly = true)
    public PageResponse<UserResponse> listUsers(String search, Pageable pageable) {
        Page<User> page = StringUtils.hasText(search)
                ? userRepository.findByUsernameContainingIgnoreCaseOrFullNameContainingIgnoreCase(search, search, pageable)
                : userRepository.findAll(pageable);
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
            throw new BusinessException("Username already exists: " + request.username());
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
        auditLogService.log("USER", "CREATE", saved.getId(), saved.getUsername(), currentUser(),
                "Email=" + saved.getEmail());
        return toUserResponse(saved);
    }

    @Override
    public UserResponse updateUser(Long id, UserUpdateRequest request) {
        User user = findUser(id);
        user.setFullName(request.fullName());
        user.setEmail(request.email());
        user.setPhone(request.phone());
        boolean wasActive = user.isActive();
        if (wasActive && !request.active()) {
            boolean isAdmin = user.getRoles().stream().anyMatch(r -> "ADMIN".equalsIgnoreCase(r.getName()));
            if (isAdmin) {
                long otherAdmins = userRepository.countActiveAdminsExcluding(user.getId());
                if (otherAdmins == 0) {
                    throw new BusinessException(
                            "Cannot deactivate this user — they are the only active Admin. " +
                            "Assign Admin role to another active user first.");
                }
            }
        }
        user.setActive(request.active());
        if (wasActive && !request.active()) {
            user.setInactiveAt(Instant.now());
        } else if (!wasActive && request.active()) {
            user.setInactiveAt(null);
        }
        if (StringUtils.hasText(request.password())) {
            user.setPasswordHash(passwordEncoder.encode(request.password()));
            auditLogService.log("USER", "PASSWORD_CHANGE", user.getId(), user.getUsername(), currentUser(), null);
        }
        user.setRoles(resolveRoles(request.roleIds()));
        user.setDirectPermissions(resolvePermissions(request.directPermissionIds()));
        user.setDepartment(resolveDepartment(request.departmentId()));
        user.setCompany(resolveCompany(request.companyId()));
        User saved = userRepository.save(user);
        auditLogService.log("USER", "UPDATE", saved.getId(), saved.getUsername(), currentUser(),
                "active=" + saved.isActive());
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
        auditLogService.log("USER", "DELETE", id, username, currentUser(), null);
    }

    // ----- MFA admin management -----

    @Override
    @Transactional(readOnly = true)
    public PageResponse<UserMfaStatusResponse> listUserMfaStatus(String search, Pageable pageable) {
        Page<User> page = StringUtils.hasText(search)
                ? userRepository.findByUsernameContainingIgnoreCaseOrFullNameContainingIgnoreCase(search, search, pageable)
                : userRepository.findAll(pageable);
        return PageResponse.from(page, u -> new UserMfaStatusResponse(
                u.getId(), u.getUsername(), u.getFullName(), u.getEmail(),
                u.isMfaEnabled(), u.getMfaSecret() != null));
    }

    @Override
    public void adminDisableMfa(Long userId) {
        User user = findUser(userId);
        user.setMfaEnabled(false);
        userRepository.save(user);
        auditLogService.log("AUTH", "MFA_DISABLED_BY_ADMIN", user.getId(), user.getUsername(), currentUser(), null);
        log.info("Admin disabled MFA for user '{}'", user.getUsername());
    }

    @Override
    public void adminResetMfa(Long userId) {
        User user = findUser(userId);
        user.setMfaEnabled(false);
        user.setMfaSecret(null);
        userRepository.save(user);
        auditLogService.log("AUTH", "MFA_RESET_BY_ADMIN", user.getId(), user.getUsername(), currentUser(), null);
        log.info("Admin reset MFA for user '{}'", user.getUsername());
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
        auditLogService.log("APPROVAL_ROUTING", "CREATE", saved.getId(), "module=" + saved.getModule(), currentUser(),
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
        auditLogService.log("APPROVAL_ROUTING", "UPDATE", saved.getId(), "module=" + saved.getModule(), currentUser(), null);
        return toRoutingResponse(saved);
    }

    @Override
    public void deleteApprovalRouting(Long id) {
        ApprovalRouting routing = approvalRoutingRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Approval routing", id));
        approvalRoutingRepository.delete(routing);
        auditLogService.log("APPROVAL_ROUTING", "DELETE", id, "id=" + id, currentUser(), null);
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
        routing.setLevel(request.level() > 0 ? request.level() : 1);
    }

    // ----- Password policy -----

    @Override
    @Transactional(readOnly = true)
    public PasswordPolicyResponse getPasswordPolicy() {
        PasswordPolicy p = passwordPolicyRepository.findAll().stream().findFirst()
                .orElse(new PasswordPolicy());
        return toPolicyResponse(p);
    }

    @Override
    public PasswordPolicyResponse updatePasswordPolicy(PasswordPolicyRequest req) {
        PasswordPolicy p = passwordPolicyRepository.findAll().stream().findFirst()
                .orElse(new PasswordPolicy());
        p.setRestrictLastPasswords(req.restrictLastPasswords());
        p.setPasswordExpiryDays(req.passwordExpiryDays());
        p.setExpiryReminderDays(req.expiryReminderDays());
        p.setSessionTimeoutHours(req.sessionTimeoutHours());
        p.setSessionTimeoutMinutes(req.sessionTimeoutMinutes());
        p.setMaxLoginAttempts(req.maxLoginAttempts());
        p.setMinPasswordLength(req.minPasswordLength());
        p.setRequireUppercase(req.requireUppercase());
        p.setRequireLowercase(req.requireLowercase());
        p.setRequireDigit(req.requireDigit());
        p.setRequireSpecialChar(req.requireSpecialChar());
        return toPolicyResponse(passwordPolicyRepository.save(p));
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
        CompanySettings s = companySettingsRepository.findAll().stream().findFirst()
                .orElse(new CompanySettings());
        applySettingsFields(s, req);
        // Upsert linked party
        Party party = s.getLinkedParty() != null
                ? s.getLinkedParty()
                : partyRepository.findByPartyCode("COMPANY001").orElse(new Party());
        applyPartyFromSettings(party, "COMPANY001", req);
        Party savedParty = partyRepository.save(party);
        s.setLinkedParty(savedParty);
        CompanySettings saved = companySettingsRepository.save(s);
        auditLogService.log("COMPANY", "UPDATE", saved.getId(), req.companyName(), currentUser(),
                "Address: " + req.city() + ", " + req.pincode());
        return toSettingsResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public CompanySettingsResponse getCompanySettingsByCompanyId(Long companyId) {
        CompanySettings s = companySettingsRepository.findByCompanyId(companyId)
                .orElse(new CompanySettings());
        return toSettingsResponse(s);
    }

    @Override
    public CompanySettingsResponse updateCompanySettingsByCompanyId(Long companyId, CompanySettingsRequest req) {
        Company company = companyRepository.findById(companyId)
                .orElseThrow(() -> new ResourceNotFoundException("Company", companyId));
        CompanySettings s = companySettingsRepository.findByCompanyId(companyId)
                .orElse(new CompanySettings());
        s.setCompany(company);
        applySettingsFields(s, req);
        Party party = s.getLinkedParty() != null
                ? s.getLinkedParty()
                : partyRepository.findByPartyCode("COMPANY" + companyId).orElse(new Party());
        applyPartyFromSettings(party, "COMPANY" + companyId, req);
        Party savedParty = partyRepository.save(party);
        s.setLinkedParty(savedParty);
        CompanySettings saved = companySettingsRepository.save(s);
        log.info("Company settings updated for company id={}", companyId);
        return toSettingsResponse(saved);
    }

    // ----- Mail config -----

    @Override
    @Transactional(readOnly = true)
    public MailConfigResponse getMailConfig() {
        CompanySettings s = companySettingsRepository.findAll().stream().findFirst().orElse(new CompanySettings());
        boolean configured = isSmtpConfigured(s);
        return new MailConfigResponse(s.getSmtpHost(), s.getSmtpPort(), s.getSmtpUsername(),
                s.getSmtpFromName(), s.getSmtpTls(), configured);
    }

    @Override
    public MailConfigResponse saveMailConfig(MailConfigRequest req) {
        CompanySettings s = companySettingsRepository.findAll().stream().findFirst().orElse(new CompanySettings());
        if (req.smtpHost() != null) s.setSmtpHost(req.smtpHost());
        if (req.smtpPort() != null) s.setSmtpPort(req.smtpPort());
        if (req.smtpUsername() != null) s.setSmtpUsername(req.smtpUsername());
        if (req.smtpPassword() != null && !req.smtpPassword().isBlank()) s.setSmtpPassword(req.smtpPassword());
        if (req.smtpFromName() != null) s.setSmtpFromName(req.smtpFromName());
        if (req.smtpTls() != null) s.setSmtpTls(req.smtpTls());
        CompanySettings saved = companySettingsRepository.save(s);
        log.info("Mail config updated: host={}", saved.getSmtpHost());
        return new MailConfigResponse(saved.getSmtpHost(), saved.getSmtpPort(), saved.getSmtpUsername(),
                saved.getSmtpFromName(), saved.getSmtpTls(), isSmtpConfigured(saved));
    }

    // ----- Company logo -----

    @Override
    public void saveCompanyLogo(Long companyId, byte[] data, String contentType) {
        Company company = companyRepository.findById(companyId)
                .orElseThrow(() -> new ResourceNotFoundException("Company", companyId));
        CompanySettings s = companySettingsRepository.findByCompanyId(companyId)
                .orElseGet(() -> { CompanySettings n = new CompanySettings(); n.setCompany(company); return n; });
        s.setLogoData(data);
        s.setLogoContentType(contentType);
        companySettingsRepository.save(s);
        log.info("Logo saved for company {}, size={} bytes", companyId, data.length);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<LogoDto> getCompanyLogo(Long companyId) {
        return companySettingsRepository.findByCompanyId(companyId)
                .filter(s -> s.getLogoData() != null && s.getLogoData().length > 0)
                .map(s -> new LogoDto(s.getLogoData(), s.getLogoContentType()));
    }

    @Override
    public void deleteCompanyLogo(Long companyId) {
        companySettingsRepository.findByCompanyId(companyId).ifPresent(s -> {
            s.setLogoData(null);
            s.setLogoContentType(null);
            companySettingsRepository.save(s);
        });
    }

    // ----- Courier ways -----

    @Override
    @Transactional(readOnly = true)
    public List<CourierWayResponse> listCourierWays() {
        return courierWayRepository.findAll().stream().map(this::toCourierWayResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<CourierWayResponse> listActiveCourierWays() {
        return courierWayRepository.findByActiveTrueOrderByNameAsc().stream().map(this::toCourierWayResponse).toList();
    }

    @Override
    public CourierWayResponse createCourierWay(CourierWayRequest req) {
        if (courierWayRepository.existsByNameIgnoreCase(req.name())) {
            throw new BusinessException("Courier way '" + req.name() + "' already exists");
        }
        CourierWay cw = new CourierWay();
        cw.setName(req.name().toUpperCase());
        cw.setActive(req.active());
        CourierWay saved = courierWayRepository.save(cw);
        auditLogService.log("COURIER_WAY", "CREATE", saved.getId(), saved.getName(), currentUser(), null);
        return toCourierWayResponse(saved);
    }

    @Override
    public CourierWayResponse updateCourierWay(Long id, CourierWayRequest req) {
        CourierWay cw = courierWayRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Courier way", id));
        if (!cw.getName().equalsIgnoreCase(req.name()) && courierWayRepository.existsByNameIgnoreCase(req.name())) {
            throw new BusinessException("Courier way '" + req.name() + "' already exists");
        }
        cw.setName(req.name().toUpperCase());
        cw.setActive(req.active());
        CourierWay saved = courierWayRepository.save(cw);
        auditLogService.log("COURIER_WAY", "UPDATE", saved.getId(), saved.getName(), currentUser(), "active=" + saved.isActive());
        return toCourierWayResponse(saved);
    }

    @Override
    public void deleteCourierWay(Long id) {
        CourierWay cw = courierWayRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Courier way", id));
        String name = cw.getName();
        courierWayRepository.delete(cw);
        auditLogService.log("COURIER_WAY", "DELETE", id, name, currentUser(), null);
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
        if (departmentRepository.existsByNameIgnoreCase(req.name())) {
            throw new BusinessException("Department '" + req.name() + "' already exists");
        }
        Department d = new Department();
        d.setName(req.name());
        d.setActive(req.active());
        Department saved = departmentRepository.save(d);
        auditLogService.log("DEPARTMENT", "CREATE", saved.getId(), saved.getName(), currentUser(), null);
        return toDeptResponse(saved);
    }

    @Override
    public DepartmentResponse updateDepartment(Long id, DepartmentRequest req) {
        Department d = departmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Department", id));
        if (!d.getName().equalsIgnoreCase(req.name()) && departmentRepository.existsByNameIgnoreCase(req.name())) {
            throw new BusinessException("Department '" + req.name() + "' already exists");
        }
        d.setName(req.name());
        d.setActive(req.active());
        Department saved = departmentRepository.save(d);
        auditLogService.log("DEPARTMENT", "UPDATE", saved.getId(), saved.getName(), currentUser(), "active=" + saved.isActive());
        return toDeptResponse(saved);
    }

    @Override
    public void deleteDepartment(Long id) {
        Department d = departmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Department", id));
        String name = d.getName();
        departmentRepository.delete(d);
        auditLogService.log("DEPARTMENT", "DELETE", id, name, currentUser(), null);
    }

    // ----- Package types -----

    @Override
    @Transactional(readOnly = true)
    public List<PackageTypeResponse> listPackageTypes() {
        return packageTypeRepository.findAll().stream().map(this::toPackageTypeResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<PackageTypeResponse> listActivePackageTypes() {
        return packageTypeRepository.findByActiveTrueOrderByNameAsc().stream().map(this::toPackageTypeResponse).toList();
    }

    @Override
    public PackageTypeResponse createPackageType(PackageTypeRequest req) {
        if (packageTypeRepository.existsByNameIgnoreCase(req.name())) {
            throw new BusinessException("Package type '" + req.name() + "' already exists");
        }
        PackageType pt = new PackageType();
        pt.setName(req.name().toUpperCase());
        pt.setActive(req.active());
        PackageType saved = packageTypeRepository.save(pt);
        auditLogService.log("PACKAGE_TYPE", "CREATE", saved.getId(), saved.getName(), currentUser(), null);
        return toPackageTypeResponse(saved);
    }

    @Override
    public PackageTypeResponse updatePackageType(Long id, PackageTypeRequest req) {
        PackageType pt = packageTypeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Package type", id));
        if (!pt.getName().equalsIgnoreCase(req.name()) && packageTypeRepository.existsByNameIgnoreCase(req.name())) {
            throw new BusinessException("Package type '" + req.name() + "' already exists");
        }
        pt.setName(req.name().toUpperCase());
        pt.setActive(req.active());
        PackageType saved = packageTypeRepository.save(pt);
        auditLogService.log("PACKAGE_TYPE", "UPDATE", saved.getId(), saved.getName(), currentUser(), "active=" + saved.isActive());
        return toPackageTypeResponse(saved);
    }

    @Override
    public void deletePackageType(Long id) {
        PackageType pt = packageTypeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Package type", id));
        String name = pt.getName();
        packageTypeRepository.delete(pt);
        auditLogService.log("PACKAGE_TYPE", "DELETE", id, name, currentUser(), null);
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
        if (companyRepository.existsByNameIgnoreCase(req.name())) {
            throw new BusinessException("Company name '" + req.name() + "' already exists");
        }
        String code = nextCompanyCode();
        Company c = Company.builder()
                .companyCode(code)
                .name(req.name())
                .active(req.active())
                .build();
        Company saved = companyRepository.save(c);
        auditLogService.log("COMPANY", "CREATE", saved.getId(), saved.getCompanyCode() + " " + saved.getName(), currentUser(), null);
        return toCompanyResponse(saved);
    }

    @Override
    public CompanyResponse updateCompany(Long id, CompanyRequest req) {
        Company c = companyRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Company", id));
        if (!c.getCompanyCode().equalsIgnoreCase(req.companyCode())
                && companyRepository.existsByCompanyCodeIgnoreCase(req.companyCode())) {
            throw new BusinessException("Company code '" + req.companyCode() + "' already exists");
        }
        if (!c.getName().equalsIgnoreCase(req.name()) && companyRepository.existsByNameIgnoreCase(req.name())) {
            throw new BusinessException("Company name '" + req.name() + "' already exists");
        }
        c.setCompanyCode(req.companyCode().toUpperCase());
        c.setName(req.name());
        c.setActive(req.active());
        Company saved = companyRepository.save(c);
        auditLogService.log("COMPANY", "UPDATE", saved.getId(), saved.getCompanyCode() + " " + saved.getName(), currentUser(),
                "active=" + saved.isActive());
        return toCompanyResponse(saved);
    }

    @Override
    public void deleteCompany(Long id) {
        Company c = companyRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Company", id));
        String name = c.getCompanyCode() + " " + c.getName();
        companyRepository.delete(c);
        auditLogService.log("COMPANY", "DELETE", id, name, currentUser(), null);
    }

    // ----- Sticker field config -----

    private static final List<StickerFieldDto> DEFAULT_FIELDS = List.of(
        new StickerFieldDto("SHIPPING_LABEL",  "Courier Shipping Label", true,  1,  "HEADER"),
        new StickerFieldDto("COMPANY_NAME",    "Company Name",           true,  2,  "HEADER"),
        new StickerFieldDto("BOOKING_NUMBER",  "Booking Number",         true,  3,  "DETAILS_LEFT"),
        new StickerFieldDto("COURIER_MODE",    "Courier Mode / Via",     true,  4,  "DETAILS_LEFT"),
        new StickerFieldDto("DETAIL_DATE",     "Date",                   true,  5,  "DETAILS_RIGHT"),
        new StickerFieldDto("DETAIL_WEIGHT",   "Weight",                 true,  6,  "DETAILS_RIGHT"),
        new StickerFieldDto("DETAIL_PACKAGES", "No. of Packages",        false, 7,  "DETAILS_RIGHT"),
        new StickerFieldDto("DETAIL_PKG_TYPE", "Package Type",           false, 8,  "DETAILS_RIGHT"),
        new StickerFieldDto("FROM_NAME",       "Creator Name",           true,  9,  "FROM"),
        new StickerFieldDto("FROM_PHONE",      "Creator Mobile",         true,  10, "FROM"),
        new StickerFieldDto("SENDER_COMPANY",  "Sender Company",         true,  11, "FROM"),
        new StickerFieldDto("FROM_ADDRESS",    "Sender Address",         true,  12, "FROM"),
        new StickerFieldDto("TO_COMPANY",      "Receiver Company",       true,  13, "TO"),
        new StickerFieldDto("TO_NAME",         "Receiver Name",          true,  14, "TO"),
        new StickerFieldDto("TO_ADDRESS",      "Receiver Address",       true,  15, "TO"),
        new StickerFieldDto("TO_PHONE",        "Receiver Phone",         true,  16, "TO"),
        new StickerFieldDto("TO_GSTIN",        "Receiver GSTIN",         false, 17, "TO"),
        new StickerFieldDto("AWB_NUMBER",      "AWB Number",             true,  18, "BOTTOM")
    );

    @Override
    @Transactional(readOnly = true)
    public List<StickerFieldDto> getStickerFieldConfig(Long companyId) {
        List<StickerFieldConfig> saved = stickerFieldConfigRepository.findByCompanyIdOrderBySortOrder(companyId);
        if (saved.isEmpty()) return DEFAULT_FIELDS;
        var savedKeys = saved.stream().map(StickerFieldConfig::getFieldKey).collect(Collectors.toSet());
        List<StickerFieldDto> result = new ArrayList<>(
            saved.stream().map(s -> {
                String section = (s.getSection() != null && !s.getSection().isBlank()) ? s.getSection()
                        : DEFAULT_FIELDS.stream().filter(d -> d.fieldKey().equals(s.getFieldKey()))
                            .map(StickerFieldDto::section).findFirst().orElse("BOTTOM");
                return new StickerFieldDto(s.getFieldKey(), s.getLabel(), s.isVisible(), s.getSortOrder(), section);
            }).toList()
        );
        int maxOrder = result.stream().mapToInt(StickerFieldDto::sortOrder).max().orElse(0);
        for (StickerFieldDto def : DEFAULT_FIELDS) {
            if (!savedKeys.contains(def.fieldKey())) {
                result.add(new StickerFieldDto(def.fieldKey(), def.label(), def.visible(), ++maxOrder, def.section()));
            }
        }
        return result;
    }

    @Override
    public List<StickerFieldDto> saveStickerFieldConfig(Long companyId, List<StickerFieldDto> fields) {
        Company company = companyRepository.findById(companyId)
                .orElseThrow(() -> new ResourceNotFoundException("Company", companyId));
        stickerFieldConfigRepository.deleteByCompanyId(companyId);
        stickerFieldConfigRepository.flush();
        List<StickerFieldConfig> entities = new ArrayList<>();
        for (StickerFieldDto dto : fields) {
            StickerFieldConfig cfg = new StickerFieldConfig();
            cfg.setCompany(company);
            cfg.setFieldKey(dto.fieldKey());
            cfg.setLabel(dto.label());
            cfg.setVisible(dto.visible());
            cfg.setSortOrder(dto.sortOrder());
            cfg.setSection(dto.section() != null ? dto.section() : "BOTTOM");
            entities.add(cfg);
        }
        stickerFieldConfigRepository.saveAll(entities);
        return getStickerFieldConfig(companyId);
    }

    // ----- Helpers -----

    private Role findRole(Long id) {
        return roleRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Role", id));
    }

    private User findUser(Long id) {
        return userRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("User", id));
    }

    private Set<Permission> resolvePermissions(Set<Long> ids) {
        if (ids == null || ids.isEmpty()) return new HashSet<>();
        Set<Permission> result = new HashSet<>(permissionRepository.findAllById(ids));
        if (result.size() != ids.size()) throw new BusinessException("One or more permission ids are invalid");
        return result;
    }

    private Set<Role> resolveRoles(Set<Long> ids) {
        if (ids == null || ids.isEmpty()) return new HashSet<>();
        Set<Role> result = new HashSet<>(roleRepository.findAllById(ids));
        if (result.size() != ids.size()) throw new BusinessException("One or more role ids are invalid");
        return result;
    }

    private Department resolveDepartment(Long id) {
        if (id == null) return null;
        return departmentRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Department", id));
    }

    private Company resolveCompany(Long id) {
        if (id == null) return null;
        return companyRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Company", id));
    }

    private String nextCompanyCode() {
        long max = companyRepository.findAll().stream()
                .map(Company::getCompanyCode)
                .filter(code -> code != null && code.matches("\\d+"))
                .mapToLong(Long::parseLong)
                .max()
                .orElse(0L);
        return String.valueOf(max + 1);
    }

    private String currentUser() {
        var auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        return (auth != null && auth.isAuthenticated()) ? auth.getName() : "system";
    }

    private RoleResponse toRoleResponse(Role role) {
        List<PermissionResponse> perms = role.getPermissions().stream()
                .sorted(Comparator.comparing(Permission::getCode))
                .map(p -> new PermissionResponse(p.getId(), p.getCode(), p.getDescription()))
                .toList();
        return new RoleResponse(role.getId(), role.getName(), role.getDescription(), role.isSystemRole(), perms);
    }

    private UserResponse toUserResponse(User user) {
        List<UserResponse.RoleSummary> roles = user.getRoles().stream()
                .sorted(Comparator.comparing(Role::getName))
                .map(r -> new UserResponse.RoleSummary(r.getId(), r.getName()))
                .toList();
        List<PermissionResponse> directPerms = user.getDirectPermissions().stream()
                .sorted(Comparator.comparing(Permission::getCode))
                .map(p -> new PermissionResponse(p.getId(), p.getCode(), p.getDescription()))
                .toList();
        Long deptId = user.getDepartment() != null ? user.getDepartment().getId() : null;
        String deptName = user.getDepartment() != null ? user.getDepartment().getName() : null;
        Long companyId = user.getCompany() != null ? user.getCompany().getId() : null;
        String companyCode = user.getCompany() != null ? user.getCompany().getCompanyCode() : null;
        String companyName = user.getCompany() != null ? user.getCompany().getName() : null;
        return new UserResponse(user.getId(), user.getUsername(), user.getFullName(), user.getEmail(),
                user.getPhone(), user.isActive(), deptId, deptName, companyId, companyCode, companyName,
                roles, directPerms, user.getInactiveAt(),
                user.getCreatedAt(), user.getCreatedBy(), user.getUpdatedAt(), user.getUpdatedBy());
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
                r.isActive(), r.getModule(), r.getLevel());
    }

    private void applySettingsFields(CompanySettings s, CompanySettingsRequest req) {
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
        if (req.smtpHost() != null) s.setSmtpHost(req.smtpHost());
        if (req.smtpPort() != null) s.setSmtpPort(req.smtpPort());
        if (req.smtpUsername() != null) s.setSmtpUsername(req.smtpUsername());
        if (req.smtpPassword() != null && !req.smtpPassword().isBlank()) s.setSmtpPassword(req.smtpPassword());
        if (req.smtpFromName() != null) s.setSmtpFromName(req.smtpFromName());
        if (req.smtpTls() != null) s.setSmtpTls(req.smtpTls());
    }

    private void applyPartyFromSettings(Party party, String partyCode, CompanySettingsRequest req) {
        party.setPartyCode(partyCode);
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
        party.setActive(true);
    }

    private CompanySettingsResponse toSettingsResponse(CompanySettings s) {
        boolean smtpConfigured = isSmtpConfigured(s);
        return new CompanySettingsResponse(s.getId(), s.getCompanyName(), s.getAddressLine1(),
                s.getAddressLine2(), s.getCity(), s.getState(), s.getPincode(),
                s.getCountry(), s.getPhone(), s.getEmail(), s.getGstin(),
                s.getSmtpHost(), s.getSmtpPort(), s.getSmtpUsername(),
                s.getSmtpFromName(), s.getSmtpTls(), smtpConfigured);
    }

    private boolean isSmtpConfigured(CompanySettings s) {
        return s.getSmtpHost() != null && !s.getSmtpHost().isBlank()
                && s.getSmtpUsername() != null && !s.getSmtpUsername().isBlank()
                && s.getSmtpPassword() != null && !s.getSmtpPassword().isBlank();
    }

    private PasswordPolicyResponse toPolicyResponse(PasswordPolicy p) {
        return new PasswordPolicyResponse(p.getId(), p.getRestrictLastPasswords(), p.getPasswordExpiryDays(),
                p.getExpiryReminderDays(), p.getSessionTimeoutHours(), p.getSessionTimeoutMinutes(),
                p.getMaxLoginAttempts(), p.getMinPasswordLength(), p.isRequireUppercase(),
                p.isRequireLowercase(), p.isRequireDigit(), p.isRequireSpecialChar());
    }

    private CourierWayResponse toCourierWayResponse(CourierWay cw) {
        return new CourierWayResponse(cw.getId(), cw.getName(), cw.isActive());
    }

    private PackageTypeResponse toPackageTypeResponse(PackageType pt) {
        return new PackageTypeResponse(pt.getId(), pt.getName(), pt.isActive());
    }

    private DepartmentResponse toDeptResponse(Department d) {
        return new DepartmentResponse(d.getId(), d.getName(), d.isActive());
    }

    private CompanyResponse toCompanyResponse(Company c) {
        return new CompanyResponse(c.getId(), c.getCompanyCode(), c.getName(), c.isActive());
    }
}
