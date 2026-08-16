package com.courierapp.service.impl;

import com.courierapp.dto.PageResponse;
import com.courierapp.dto.admin.*;

import java.util.Optional;
import com.courierapp.entity.ApprovalRouting;
import com.courierapp.entity.Company;
import com.courierapp.entity.CompanySettings;
import com.courierapp.entity.CourierWay;
import com.courierapp.entity.Department;
import com.courierapp.entity.PackageType;
import com.courierapp.entity.Party;
import com.courierapp.entity.Permission;
import com.courierapp.entity.Role;
import com.courierapp.entity.StickerFieldConfig;
import com.courierapp.entity.Unit;
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
import com.courierapp.repository.UnitRepository;
import com.courierapp.repository.UserRepository;
import com.courierapp.service.AdminService;
import com.courierapp.service.AuditLogService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import static com.courierapp.config.CacheConfig.*;

import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

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
    private final com.courierapp.repository.PasswordPolicyRepository passwordPolicyRepository;
    private final com.courierapp.repository.StickerFieldConfigRepository stickerFieldConfigRepository;
    private final UnitRepository unitRepository;

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
                            AuditLogService auditLogService,
                            com.courierapp.repository.PasswordPolicyRepository passwordPolicyRepository,
                            com.courierapp.repository.StickerFieldConfigRepository stickerFieldConfigRepository,
                            UnitRepository unitRepository) {
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
        this.passwordPolicyRepository = passwordPolicyRepository;
        this.stickerFieldConfigRepository = stickerFieldConfigRepository;
        this.unitRepository = unitRepository;
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
        auditLogService.log("ROLE", "CREATE", saved.getId(), saved.getName(), currentUsername(),
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
        auditLogService.log("ROLE", "UPDATE", saved.getId(), saved.getName(), currentUsername(),
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
        auditLogService.log("ROLE", "DELETE", id, name, currentUsername(), null);
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
        auditLogService.log("USER", "CREATE", saved.getId(), saved.getUsername(), currentUsername(),
                "Email=" + saved.getEmail() + ", department=" + (saved.getDepartment() != null ? saved.getDepartment().getName() : "none"));
        return toUserResponse(saved);
    }

    @Override
    public UserResponse updateUser(Long id, UserUpdateRequest request) {
        User user = findUser(id);
        user.setFullName(request.fullName());
        user.setEmail(request.email());
        user.setPhone(request.phone());
        boolean wasActive = user.isActive();
        // Prevent deactivating the last active admin
        if (wasActive && !request.active()) {
            boolean userIsAdmin = user.getRoles().stream()
                    .anyMatch(r -> "ADMIN".equalsIgnoreCase(r.getName()));
            if (userIsAdmin) {
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
            user.setInactiveAt(java.time.Instant.now());
        } else if (!wasActive && request.active()) {
            user.setInactiveAt(null); // re-activated
        }
        if (StringUtils.hasText(request.password())) {
            user.setPasswordHash(passwordEncoder.encode(request.password()));
            auditLogService.log("USER", "PASSWORD_CHANGE", user.getId(), user.getUsername(), currentUsername(), null);
        }
        user.setRoles(resolveRoles(request.roleIds()));
        user.setDirectPermissions(resolvePermissions(request.directPermissionIds()));
        user.setDepartment(resolveDepartment(request.departmentId()));
        user.setCompany(resolveCompany(request.companyId()));
        User saved = userRepository.save(user);
        auditLogService.log("USER", "UPDATE", saved.getId(), saved.getUsername(), currentUsername(),
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
        auditLogService.log("USER", "DELETE", id, username, currentUsername(), null);
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
        auditLogService.log("APPROVAL_ROUTING", "CREATE", saved.getId(), "module=" + saved.getModule(), currentUsername(),
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
        auditLogService.log("APPROVAL_ROUTING", "UPDATE", saved.getId(), "module=" + saved.getModule(), currentUsername(), null);
        return toRoutingResponse(saved);
    }

    @Override
    public void deleteApprovalRouting(Long id) {
        ApprovalRouting routing = approvalRoutingRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Approval routing", id));
        approvalRoutingRepository.delete(routing);
        auditLogService.log("APPROVAL_ROUTING", "DELETE", id, "id=" + id, currentUsername(), null);
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
                user.getInactiveAt(),
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
                r.getModule(),
                r.getLevel());
    }

    // ----- Company settings -----

    @Override
    @Transactional(readOnly = true)
    @Cacheable(CACHE_COMPANY_SETTINGS)
    public CompanySettingsResponse getCompanySettings() {
        CompanySettings s = companySettingsRepository.findAll().stream().findFirst()
                .orElseThrow(() -> new BusinessException("Company settings not found"));
        return toSettingsResponse(s);
    }

    @Override
    @CacheEvict(value = CACHE_COMPANY_SETTINGS, allEntries = true)
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
        // SMTP config — only overwrite password if a new one is provided
        if (req.smtpHost() != null) s.setSmtpHost(req.smtpHost());
        if (req.smtpPort() != null) s.setSmtpPort(req.smtpPort());
        if (req.smtpUsername() != null) s.setSmtpUsername(req.smtpUsername());
        if (req.smtpPassword() != null && !req.smtpPassword().isBlank()) s.setSmtpPassword(req.smtpPassword());
        if (req.smtpFromName() != null) s.setSmtpFromName(req.smtpFromName());
        if (req.smtpTls() != null) s.setSmtpTls(req.smtpTls());

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
        auditLogService.log("COMPANY", "UPDATE", saved.getId(), req.companyName(), currentUsername(),
                "Address: " + req.city() + ", " + req.pincode());
        return toSettingsResponse(saved);
    }

    private CompanySettingsResponse toSettingsResponse(CompanySettings s) {
        boolean smtpConfigured = s.getSmtpHost() != null && !s.getSmtpHost().isBlank()
                && s.getSmtpUsername() != null && !s.getSmtpUsername().isBlank()
                && s.getSmtpPassword() != null && !s.getSmtpPassword().isBlank();
        return new CompanySettingsResponse(s.getId(), s.getCompanyName(), s.getAddressLine1(),
                s.getAddressLine2(), s.getCity(), s.getState(), s.getPincode(),
                s.getCountry(), s.getPhone(), s.getEmail(), s.getGstin(),
                s.getSmtpHost(), s.getSmtpPort(), s.getSmtpUsername(),
                s.getSmtpFromName(), s.getSmtpTls(), smtpConfigured);
    }

    // ----- Mail configuration (global) -----

    @Override
    @Transactional(readOnly = true)
    public MailConfigResponse getMailConfig() {
        CompanySettings s = companySettingsRepository.findAll().stream().findFirst().orElse(new CompanySettings());
        boolean configured = s.getSmtpHost() != null && !s.getSmtpHost().isBlank()
                && s.getSmtpUsername() != null && !s.getSmtpUsername().isBlank()
                && s.getSmtpPassword() != null && !s.getSmtpPassword().isBlank();
        return new MailConfigResponse(s.getSmtpHost(), s.getSmtpPort(), s.getSmtpUsername(),
                s.getSmtpFromName(), s.getSmtpTls(), configured);
    }

    @Override
    public MailConfigResponse saveMailConfig(MailConfigRequest req) {
        CompanySettings s = companySettingsRepository.findAll().stream().findFirst()
                .orElse(new CompanySettings());
        if (req.smtpHost() != null) s.setSmtpHost(req.smtpHost());
        if (req.smtpPort() != null) s.setSmtpPort(req.smtpPort());
        if (req.smtpUsername() != null) s.setSmtpUsername(req.smtpUsername());
        if (req.smtpPassword() != null && !req.smtpPassword().isBlank()) s.setSmtpPassword(req.smtpPassword());
        if (req.smtpFromName() != null) s.setSmtpFromName(req.smtpFromName());
        if (req.smtpTls() != null) s.setSmtpTls(req.smtpTls());
        CompanySettings saved = companySettingsRepository.save(s);
        log.info("Mail config updated: host={} user={}", saved.getSmtpHost(), saved.getSmtpUsername());
        boolean configured = saved.getSmtpHost() != null && !saved.getSmtpHost().isBlank()
                && saved.getSmtpUsername() != null && !saved.getSmtpUsername().isBlank()
                && saved.getSmtpPassword() != null && !saved.getSmtpPassword().isBlank();
        return new MailConfigResponse(saved.getSmtpHost(), saved.getSmtpPort(), saved.getSmtpUsername(),
                saved.getSmtpFromName(), saved.getSmtpTls(), configured);
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

    @Override
    @Transactional(readOnly = true)
    public CompanySettingsResponse getCompanySettingsByCompanyId(Long companyId) {
        CompanySettings s = companySettingsRepository.findByCompanyId(companyId)
                .orElse(new CompanySettings()); // empty if not yet configured
        return toSettingsResponse(s);
    }

    @Override
    public CompanySettingsResponse updateCompanySettingsByCompanyId(Long companyId, CompanySettingsRequest req) {
        Company company = companyRepository.findById(companyId)
                .orElseThrow(() -> new ResourceNotFoundException("Company", companyId));
        CompanySettings s = companySettingsRepository.findByCompanyId(companyId)
                .orElse(new CompanySettings());
        s.setCompany(company);
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
        // Upsert the linked company party (sender)
        Party party = s.getLinkedParty() != null
                ? s.getLinkedParty()
                : partyRepository.findByPartyCode("COMPANY" + companyId).orElse(new Party());
        party.setPartyCode("COMPANY" + companyId);
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
        party.setCompany(company);
        Party savedParty = partyRepository.save(party);
        s.setLinkedParty(savedParty);
        CompanySettings saved = companySettingsRepository.save(s);
        log.info("Company settings updated for company id={}", companyId);
        return toSettingsResponse(saved);
    }

    // ----- Password policy -----

    @Override
    @Transactional(readOnly = true)
    public com.courierapp.dto.admin.PasswordPolicyResponse getPasswordPolicy() {
        com.courierapp.entity.PasswordPolicy p = passwordPolicyRepository.findAll().stream().findFirst()
                .orElse(defaultPolicy());
        return toPolicyResponse(p);
    }

    @Override
    public com.courierapp.dto.admin.PasswordPolicyResponse updatePasswordPolicy(
            com.courierapp.dto.admin.PasswordPolicyRequest req) {
        com.courierapp.entity.PasswordPolicy p = passwordPolicyRepository.findAll().stream().findFirst()
                .orElse(new com.courierapp.entity.PasswordPolicy());
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

    private com.courierapp.entity.PasswordPolicy defaultPolicy() {
        com.courierapp.entity.PasswordPolicy p = new com.courierapp.entity.PasswordPolicy();
        return p;
    }

    private com.courierapp.dto.admin.PasswordPolicyResponse toPolicyResponse(
            com.courierapp.entity.PasswordPolicy p) {
        return new com.courierapp.dto.admin.PasswordPolicyResponse(
                p.getId(),
                p.getRestrictLastPasswords(), p.getPasswordExpiryDays(), p.getExpiryReminderDays(),
                p.getSessionTimeoutHours(), p.getSessionTimeoutMinutes(), p.getMaxLoginAttempts(),
                p.getMinPasswordLength(), p.isRequireUppercase(), p.isRequireLowercase(),
                p.isRequireDigit(), p.isRequireSpecialChar());
    }

    // ----- Courier ways -----

    @Override
    @Transactional(readOnly = true)
    @Cacheable(CACHE_COURIER_WAYS)
    public List<CourierWayResponse> listCourierWays() {
        return courierWayRepository.findAll().stream()
                .map(this::toCourierWayResponse).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = CACHE_COURIER_WAYS, key = "'active'")
    public List<CourierWayResponse> listActiveCourierWays() {
        return courierWayRepository.findByActiveTrueOrderByNameAsc().stream()
                .map(this::toCourierWayResponse).collect(Collectors.toList());
    }

    @Override
    @CacheEvict(value = CACHE_COURIER_WAYS, allEntries = true)
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
        auditLogService.log("COURIER_WAY", "CREATE", saved.getId(), saved.getName(), currentUsername(), null);
        return toCourierWayResponse(saved);
    }

    @Override
    @CacheEvict(value = CACHE_COURIER_WAYS, allEntries = true)
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
        auditLogService.log("COURIER_WAY", "UPDATE", saved.getId(), saved.getName(), currentUsername(),
                "active=" + saved.isActive());
        return toCourierWayResponse(saved);
    }

    @Override
    @CacheEvict(value = CACHE_COURIER_WAYS, allEntries = true)
    public void deleteCourierWay(Long id) {
        log.info("Deleting courier way id={}", id);
        CourierWay cw = courierWayRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Courier way", id));
        String name = cw.getName();
        courierWayRepository.delete(cw);
        auditLogService.log("COURIER_WAY", "DELETE", id, name, currentUsername(), null);
    }

    private CourierWayResponse toCourierWayResponse(CourierWay cw) {
        return new CourierWayResponse(cw.getId(), cw.getName(), cw.isActive());
    }

    // ----- Package types -----

    @Override
    @Transactional(readOnly = true)
    @Cacheable(CACHE_PACKAGE_TYPES)
    public List<PackageTypeResponse> listPackageTypes() {
        return packageTypeRepository.findAll().stream()
                .map(this::toPackageTypeResponse).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = CACHE_PACKAGE_TYPES, key = "'active'")
    public List<PackageTypeResponse> listActivePackageTypes() {
        return packageTypeRepository.findByActiveTrueOrderByNameAsc().stream()
                .map(this::toPackageTypeResponse).collect(Collectors.toList());
    }

    @Override
    @CacheEvict(value = CACHE_PACKAGE_TYPES, allEntries = true)
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
        auditLogService.log("PACKAGE_TYPE", "CREATE", saved.getId(), saved.getName(), currentUsername(), null);
        return toPackageTypeResponse(saved);
    }

    @Override
    @CacheEvict(value = CACHE_PACKAGE_TYPES, allEntries = true)
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
        auditLogService.log("PACKAGE_TYPE", "UPDATE", saved.getId(), saved.getName(), currentUsername(),
                "active=" + saved.isActive());
        return toPackageTypeResponse(saved);
    }

    @Override
    @CacheEvict(value = CACHE_PACKAGE_TYPES, allEntries = true)
    public void deletePackageType(Long id) {
        log.info("Deleting package type id={}", id);
        PackageType pt = packageTypeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Package type", id));
        String name = pt.getName();
        packageTypeRepository.delete(pt);
        auditLogService.log("PACKAGE_TYPE", "DELETE", id, name, currentUsername(), null);
    }

    private PackageTypeResponse toPackageTypeResponse(PackageType pt) {
        return new PackageTypeResponse(pt.getId(), pt.getName(), pt.isActive());
    }

    // ----- Departments -----

    @Override
    @Transactional(readOnly = true)
    @Cacheable(CACHE_DEPARTMENTS)
    public List<DepartmentResponse> listDepartments() {
        return departmentRepository.findAll().stream().map(this::toDeptResponse).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = CACHE_DEPARTMENTS, key = "'active'")
    public List<DepartmentResponse> listActiveDepartments() {
        return departmentRepository.findByActiveTrueOrderByNameAsc().stream().map(this::toDeptResponse).collect(Collectors.toList());
    }

    @Override
    @CacheEvict(value = CACHE_DEPARTMENTS, allEntries = true)
    public DepartmentResponse createDepartment(DepartmentRequest req) {
        log.info("Creating department: name={}", req.name());
        if (departmentRepository.existsByNameIgnoreCase(req.name())) {
            throw new BusinessException("Department '" + req.name() + "' already exists");
        }
        Department d = new Department();
        d.setName(req.name());
        d.setActive(req.active());
        Department saved = departmentRepository.save(d);
        auditLogService.log("DEPARTMENT", "CREATE", saved.getId(), saved.getName(), currentUsername(), null);
        return toDeptResponse(saved);
    }

    @Override
    @CacheEvict(value = CACHE_DEPARTMENTS, allEntries = true)
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
        auditLogService.log("DEPARTMENT", "UPDATE", saved.getId(), saved.getName(), currentUsername(),
                "active=" + saved.isActive());
        return toDeptResponse(saved);
    }

    @Override
    @CacheEvict(value = CACHE_DEPARTMENTS, allEntries = true)
    public void deleteDepartment(Long id) {
        log.info("Deleting department id={}", id);
        Department d = departmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Department", id));
        String name = d.getName();
        departmentRepository.delete(d);
        auditLogService.log("DEPARTMENT", "DELETE", id, name, currentUsername(), null);
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
        log.info("Creating company: name={}", req.name());
        if (companyRepository.existsByNameIgnoreCase(req.name())) {
            throw new BusinessException("Company name '" + req.name() + "' already exists");
        }
        // Auto-generate sequential numeric code: 1, 2, 3 ...
        String code = nextCompanyCode();
        Company c = Company.builder()
                .companyCode(code)
                .name(req.name())
                .active(req.active())
                .build();
        Company saved = companyRepository.save(c);
        auditLogService.log("COMPANY", "CREATE", saved.getId(), saved.getCompanyCode() + " " + saved.getName(), currentUsername(), null);
        return toCompanyResponse(saved);
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
        auditLogService.log("COMPANY", "UPDATE", saved.getId(), saved.getCompanyCode() + " " + saved.getName(), currentUsername(),
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
        auditLogService.log("COMPANY", "DELETE", id, name, currentUsername(), null);
    }

    private CompanyResponse toCompanyResponse(Company c) {
        return new CompanyResponse(c.getId(), c.getCompanyCode(), c.getName(), c.isActive());
    }

    // ----- Sticker field config -----

    // Ordered list of all configurable sticker fields.
    // Layout: HEADER (centered: Shipping Label + Company Name)
    // Sticker layout (top → bottom):
    //   HEADER       — centered: "COURIER SHIPPING LABEL" + Company Name
    //   DETAILS_LEFT — Booking No + Courier Mode
    //   DETAILS_RIGHT— Date + Weight (top-right)
    //   FROM         — Creator Name → Mobile → Company Name → Address
    //   TO           — Company / Name (large) / Address / Phone / GSTIN
    //   BOTTOM       — AWB Number (large, full width)
    private static final List<StickerFieldDto> DEFAULT_FIELDS = List.of(
        // ── Header (centered) ──
        new StickerFieldDto("SHIPPING_LABEL",  "Courier Shipping Label", true,  1,  "HEADER"),
        new StickerFieldDto("COMPANY_NAME",    "Company Name",           true,  2,  "HEADER"),
        // ── Details left column ──
        new StickerFieldDto("BOOKING_NUMBER",  "Booking Number",         true,  3,  "DETAILS_LEFT"),
        new StickerFieldDto("COURIER_MODE",    "Courier Mode / Via",     true,  4,  "DETAILS_LEFT"),
        // ── Details right column (Date / Weight) ──
        new StickerFieldDto("DETAIL_DATE",     "Date",                   true,  5,  "DETAILS_RIGHT"),
        new StickerFieldDto("DETAIL_WEIGHT",   "Weight",                 true,  6,  "DETAILS_RIGHT"),
        new StickerFieldDto("DETAIL_PACKAGES", "No. of Packages",        false, 7,  "DETAILS_RIGHT"),
        new StickerFieldDto("DETAIL_PKG_TYPE", "Package Type",           false, 8,  "DETAILS_RIGHT"),
        // ── FROM (Sender) — Creator first, then company ──
        new StickerFieldDto("FROM_NAME",       "Creator Name",           true,  9,  "FROM"),
        new StickerFieldDto("FROM_PHONE",      "Creator Mobile",         true,  10, "FROM"),
        new StickerFieldDto("SENDER_COMPANY",  "Sender Company",         true,  11, "FROM"),
        new StickerFieldDto("FROM_ADDRESS",    "Sender Address",         true,  12, "FROM"),
        // ── TO (Receiver) ──
        new StickerFieldDto("TO_COMPANY",      "Receiver Company",       true,  13, "TO"),
        new StickerFieldDto("TO_NAME",         "Receiver Name",          true,  14, "TO"),
        new StickerFieldDto("TO_ADDRESS",      "Receiver Address",       true,  15, "TO"),
        new StickerFieldDto("TO_PHONE",        "Receiver Phone",         true,  16, "TO"),
        new StickerFieldDto("TO_GSTIN",        "Receiver GSTIN",         false, 17, "TO"),
        // ── Bottom — AWB Number (large, full width) ──
        new StickerFieldDto("AWB_NUMBER",      "AWB Number",             true,  18, "BOTTOM")
    );

    @Override
    @Transactional(readOnly = true)
    public List<StickerFieldDto> getStickerFieldConfig(Long companyId) {
        List<StickerFieldConfig> saved = stickerFieldConfigRepository.findByCompanyIdOrderBySortOrder(companyId);
        if (saved.isEmpty()) {
            return DEFAULT_FIELDS;
        }
        var savedKeys = saved.stream().map(StickerFieldConfig::getFieldKey).collect(java.util.stream.Collectors.toSet());
        List<StickerFieldDto> result = new java.util.ArrayList<>(
            saved.stream().map(s -> {
                // Use DB-persisted section; fall back to default only for legacy rows with no section
                String section = (s.getSection() != null && !s.getSection().isBlank())
                    ? s.getSection()
                    : DEFAULT_FIELDS.stream()
                        .filter(d -> d.fieldKey().equals(s.getFieldKey()))
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
        List<StickerFieldConfig> entities = new java.util.ArrayList<>();
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

    private String currentUsername() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return (auth != null && auth.isAuthenticated()) ? auth.getName() : "system";
    }

    private Long currentCompanyId() {
        return userRepository.findByUsername(currentUsername())
                .map(User::getCompany)
                .map(Company::getId)
                .orElseThrow(() -> new BusinessException("Current user has no company assigned"));
    }

    // ----- Units -----

    @Override
    @Transactional(readOnly = true)
    public List<UnitResponse> listUnits() {
        return unitRepository.findByCompanyIdOrderByUnitNameAsc(currentCompanyId()).stream()
                .map(this::toUnitResponse).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<UnitResponse> listActiveUnits() {
        return unitRepository.findByCompanyIdAndActiveTrueOrderByUnitNameAsc(currentCompanyId()).stream()
                .map(this::toUnitResponse).collect(Collectors.toList());
    }

    @Override
    public UnitResponse createUnit(UnitRequest req) {
        Long companyId = currentCompanyId();
        log.info("Creating unit: companyId={}, name={}", companyId, req.unitName());
        if (unitRepository.existsByCompanyIdAndUnitNameIgnoreCase(companyId, req.unitName())) {
            throw new BusinessException("Unit '" + req.unitName() + "' already exists");
        }
        Company company = companyRepository.findById(companyId)
                .orElseThrow(() -> new ResourceNotFoundException("Company", companyId));
        Unit unit = new Unit();
        unit.setCompany(company);
        applyUnitFields(unit, req);
        if (req.defaultUnit()) {
            clearExistingDefault(companyId);
        }
        Unit saved = unitRepository.save(unit);
        log.info("Unit created: id={}, name={}", saved.getId(), saved.getUnitName());
        auditLogService.log("UNIT", "CREATE", saved.getId(), saved.getUnitName(), currentUsername(), null);
        return toUnitResponse(saved);
    }

    @Override
    public UnitResponse updateUnit(Long id, UnitRequest req) {
        Long companyId = currentCompanyId();
        log.info("Updating unit id={}: name={}", id, req.unitName());
        Unit unit = unitRepository.findById(id)
                .filter(u -> u.getCompany() != null && companyId.equals(u.getCompany().getId()))
                .orElseThrow(() -> new ResourceNotFoundException("Unit", id));
        if (!unit.getUnitName().equalsIgnoreCase(req.unitName())
                && unitRepository.existsByCompanyIdAndUnitNameIgnoreCase(companyId, req.unitName())) {
            throw new BusinessException("Unit '" + req.unitName() + "' already exists");
        }
        applyUnitFields(unit, req);
        if (req.defaultUnit()) {
            clearExistingDefault(companyId);
            unit.setDefaultUnit(true);
        }
        Unit saved = unitRepository.save(unit);
        auditLogService.log("UNIT", "UPDATE", saved.getId(), saved.getUnitName(), currentUsername(),
                "active=" + saved.isActive());
        return toUnitResponse(saved);
    }

    @Override
    public void deleteUnit(Long id) {
        Long companyId = currentCompanyId();
        log.info("Deleting unit id={}", id);
        Unit unit = unitRepository.findById(id)
                .filter(u -> u.getCompany() != null && companyId.equals(u.getCompany().getId()))
                .orElseThrow(() -> new ResourceNotFoundException("Unit", id));
        String name = unit.getUnitName();
        unitRepository.delete(unit);
        auditLogService.log("UNIT", "DELETE", id, name, currentUsername(), null);
    }

    private void applyUnitFields(Unit unit, UnitRequest req) {
        unit.setUnitName(req.unitName());
        unit.setAddressLine1(req.addressLine1());
        unit.setAddressLine2(req.addressLine2());
        unit.setCity(req.city());
        unit.setState(req.state());
        unit.setPincode(req.pincode());
        unit.setCountry(req.country());
        unit.setPhone(req.phone());
        unit.setEmail(req.email());
        unit.setGstin(req.gstin());
        unit.setActive(req.active());
    }

    /** Only one unit per company may be marked default. */
    private void clearExistingDefault(Long companyId) {
        unitRepository.findByCompanyIdAndDefaultUnitTrue(companyId)
                .ifPresent(existing -> {
                    existing.setDefaultUnit(false);
                    unitRepository.save(existing);
                });
    }

    private UnitResponse toUnitResponse(Unit u) {
        return new UnitResponse(u.getId(), u.getUnitName(), u.getAddressLine1(), u.getAddressLine2(),
                u.getCity(), u.getState(), u.getPincode(), u.getCountry(), u.getPhone(), u.getEmail(),
                u.getGstin(), u.isDefaultUnit(), u.isActive());
    }
}
