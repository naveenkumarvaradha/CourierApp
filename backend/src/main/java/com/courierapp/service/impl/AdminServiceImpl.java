package com.courierapp.service.impl;

import com.courierapp.dto.PageResponse;
import com.courierapp.dto.admin.*;
import com.courierapp.entity.ApprovalRouting;
import com.courierapp.entity.Permission;
import com.courierapp.entity.Role;
import com.courierapp.entity.User;
import com.courierapp.exception.BusinessException;
import com.courierapp.exception.DuplicateResourceException;
import com.courierapp.exception.ResourceNotFoundException;
import com.courierapp.mapper.PermissionMapper;
import com.courierapp.repository.ApprovalRoutingRepository;
import com.courierapp.repository.PermissionRepository;
import com.courierapp.repository.RoleRepository;
import com.courierapp.repository.UserRepository;
import com.courierapp.service.AdminService;
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

@Service
@Transactional
public class AdminServiceImpl implements AdminService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;
    private final ApprovalRoutingRepository approvalRoutingRepository;
    private final PasswordEncoder passwordEncoder;
    private final PermissionMapper permissionMapper;

    public AdminServiceImpl(UserRepository userRepository,
                            RoleRepository roleRepository,
                            PermissionRepository permissionRepository,
                            ApprovalRoutingRepository approvalRoutingRepository,
                            PasswordEncoder passwordEncoder,
                            PermissionMapper permissionMapper) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.permissionRepository = permissionRepository;
        this.approvalRoutingRepository = approvalRoutingRepository;
        this.passwordEncoder = passwordEncoder;
        this.permissionMapper = permissionMapper;
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
        return toRoleResponse(roleRepository.save(role));
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
        return toRoleResponse(roleRepository.save(role));
    }

    @Override
    public void deleteRole(Long id) {
        Role role = findRole(id);
        if (role.isSystemRole()) {
            throw new BusinessException("System roles cannot be deleted");
        }
        roleRepository.delete(role);
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
        return toUserResponse(userRepository.save(user));
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
        }
        user.setRoles(resolveRoles(request.roleIds()));
        user.setDirectPermissions(resolvePermissions(request.directPermissionIds()));
        return toUserResponse(userRepository.save(user));
    }

    @Override
    public void deleteUser(Long id) {
        User user = findUser(id);
        if ("admin".equalsIgnoreCase(user.getUsername())) {
            throw new BusinessException("The default admin user cannot be deleted");
        }
        userRepository.delete(user);
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
        return toRoutingResponse(approvalRoutingRepository.save(routing));
    }

    @Override
    public ApprovalRoutingResponse updateApprovalRouting(Long id, ApprovalRoutingRequest request) {
        validateRouting(request);
        ApprovalRouting routing = approvalRoutingRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Approval routing", id));
        applyRouting(routing, request);
        return toRoutingResponse(approvalRoutingRepository.save(routing));
    }

    @Override
    public void deleteApprovalRouting(Long id) {
        ApprovalRouting routing = approvalRoutingRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Approval routing", id));
        approvalRoutingRepository.delete(routing);
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
        routing.setActive(request.active());
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
        return new UserResponse(user.getId(), user.getUsername(), user.getFullName(), user.getEmail(),
                user.getPhone(), user.isActive(), roles, directPerms,
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
                r.isActive());
    }
}
