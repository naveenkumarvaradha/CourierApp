package com.courierapp.service;

import com.courierapp.entity.ApprovalRouting;
import com.courierapp.entity.User;
import com.courierapp.repository.ApprovalRoutingRepository;
import com.courierapp.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
public class ApprovalAuthorizationService {

    private final ApprovalRoutingRepository approvalRoutingRepository;
    private final UserRepository userRepository;

    public ApprovalAuthorizationService(ApprovalRoutingRepository approvalRoutingRepository,
                                        UserRepository userRepository) {
        this.approvalRoutingRepository = approvalRoutingRepository;
        this.userRepository = userRepository;
    }

    /** Backward-compatible: checks level 1 for BOOKING. */
    @Transactional(readOnly = true)
    public boolean isAuthorizedApprover(String approverUsername, String creatorUsername) {
        return isAuthorizedApproverAtLevel(approverUsername, creatorUsername, "BOOKING", 1);
    }

    /** Backward-compatible: checks level 1 for any module. */
    @Transactional(readOnly = true)
    public boolean isAuthorizedApprover(String approverUsername, String creatorUsername, String module) {
        return isAuthorizedApproverAtLevel(approverUsername, creatorUsername, module, 1);
    }

    /**
     * Returns true if {@code approverUsername} is a designated approver for {@code module}
     * at the specified {@code level} when the record was created by {@code creatorUsername}.
     */
    /** Returns true if the user has the ADMIN_VIEW permission (admin bypass). */
    @Transactional(readOnly = true)
    public boolean isAdmin(String username) {
        User user = userRepository.findByUsername(username).orElse(null);
        if (user == null) return false;
        return user.getRoles().stream()
                .flatMap(r -> r.getPermissions().stream())
                .anyMatch(p -> "ADMIN_VIEW".equals(p.getCode()));
    }

    /**
     * Checks if the user is designated as approver via routing rules ONLY — no admin bypass.
     * Used in dashboard task list so admins only see what's actually routed to them.
     */
    @Transactional(readOnly = true)
    public boolean isDesignatedApproverAtLevel(String approverUsername, String creatorUsername,
                                               String module, int level) {
        User approver = userRepository.findByUsername(approverUsername).orElse(null);
        if (approver == null) return false;

        Set<String> approverRoles = approver.getRoles().stream()
                .map(r -> r.getName()).collect(Collectors.toSet());

        Set<String> creatorRoles = Set.of();
        if (creatorUsername != null) {
            User creator = userRepository.findByUsername(creatorUsername).orElse(null);
            if (creator != null) {
                creatorRoles = creator.getRoles().stream()
                        .map(r -> r.getName()).collect(Collectors.toSet());
            }
        }
        final Set<String> resolvedCreatorRoles = creatorRoles;

        List<ApprovalRouting> activeRoutes = approvalRoutingRepository.findByActiveTrue().stream()
                .filter(r -> module.equalsIgnoreCase(r.getModule()) && r.getLevel() == level)
                .toList();

        for (ApprovalRouting routing : activeRoutes) {
            if (!matchesCreator(routing, creatorUsername, resolvedCreatorRoles)) continue;
            if (routing.getUser() != null
                    && routing.getUser().getUsername().equalsIgnoreCase(approverUsername)) {
                return true;
            }
            if (routing.getRole() != null
                    && approverRoles.contains(routing.getRole().getName())) {
                return true;
            }
        }
        return false;
    }

    @Transactional(readOnly = true)
    public boolean isAuthorizedApproverAtLevel(String approverUsername, String creatorUsername,
                                               String module, int level) {
        log.debug("Authorization check: approver={}, creator={}, module={}, level={}",
                approverUsername, creatorUsername, module, level);

        User approver = userRepository.findByUsername(approverUsername).orElse(null);
        if (approver == null) {
            log.warn("Approver '{}' not found in system", approverUsername);
            return false;
        }

        // Admin users can approve/reject any record at any level
        Set<String> approverPerms = approver.getRoles().stream()
                .flatMap(r -> r.getPermissions().stream())
                .map(p -> p.getCode()).collect(Collectors.toSet());
        if (approverPerms.contains("ADMIN_VIEW")) {
            log.debug("Admin bypass: '{}' granted approval authority", approverUsername);
            return true;
        }

        Set<String> approverRoles = approver.getRoles().stream()
                .map(r -> r.getName()).collect(Collectors.toSet());

        Set<String> creatorRoles = Set.of();
        if (creatorUsername != null) {
            User creator = userRepository.findByUsername(creatorUsername).orElse(null);
            if (creator != null) {
                creatorRoles = creator.getRoles().stream()
                        .map(r -> r.getName()).collect(Collectors.toSet());
            }
        }
        final Set<String> resolvedCreatorRoles = creatorRoles;

        List<ApprovalRouting> activeRoutes = approvalRoutingRepository.findByActiveTrue().stream()
                .filter(r -> module.equalsIgnoreCase(r.getModule()) && r.getLevel() == level)
                .toList();

        for (ApprovalRouting routing : activeRoutes) {
            if (!matchesCreator(routing, creatorUsername, resolvedCreatorRoles)) continue;

            if (routing.getUser() != null
                    && routing.getUser().getUsername().equalsIgnoreCase(approverUsername)) {
                log.debug("Authorized via user-specific rule id={}, level={}", routing.getId(), level);
                return true;
            }
            if (routing.getRole() != null
                    && approverRoles.contains(routing.getRole().getName())) {
                log.debug("Authorized via role rule id={}, role={}, level={}", routing.getId(),
                        routing.getRole().getName(), level);
                return true;
            }
        }

        log.warn("Authorization DENIED: approver='{}' is not designated for module={} level={} created by '{}'",
                approverUsername, module, level, creatorUsername);
        return false;
    }

    /**
     * Returns the maximum approval level configured for the given module and creator.
     * Returns 1 if no multi-level routing is configured.
     */
    @Transactional(readOnly = true)
    public int getMaxLevel(String creatorUsername, String module) {
        Set<String> creatorRoles = Set.of();
        if (creatorUsername != null) {
            User creator = userRepository.findByUsername(creatorUsername).orElse(null);
            if (creator != null) {
                creatorRoles = creator.getRoles().stream()
                        .map(r -> r.getName()).collect(Collectors.toSet());
            }
        }
        final Set<String> resolvedCreatorRoles = creatorRoles;

        return approvalRoutingRepository.findByActiveTrue().stream()
                .filter(r -> module.equalsIgnoreCase(r.getModule()))
                .filter(r -> matchesCreator(r, creatorUsername, resolvedCreatorRoles))
                .mapToInt(ApprovalRouting::getLevel)
                .max()
                .orElse(1);
    }

    /**
     * Returns human-readable approver descriptions at the given level for display in UI.
     * Format: ["User: manager", "Role: Approval Manager"]
     */
    @Transactional(readOnly = true)
    public List<String> resolveApproversAtLevel(String creatorUsername, String module, int level) {
        Set<String> creatorRoles = Set.of();
        if (creatorUsername != null) {
            User creator = userRepository.findByUsername(creatorUsername).orElse(null);
            if (creator != null) {
                creatorRoles = creator.getRoles().stream()
                        .map(r -> r.getName()).collect(Collectors.toSet());
            }
        }
        final Set<String> resolvedCreatorRoles = creatorRoles;

        List<String> approvers = new ArrayList<>();
        approvalRoutingRepository.findByActiveTrue().stream()
                .filter(r -> module.equalsIgnoreCase(r.getModule()) && r.getLevel() == level)
                .filter(r -> matchesCreator(r, creatorUsername, resolvedCreatorRoles))
                .forEach(r -> {
                    if (r.getUser() != null) {
                        String display = r.getUser().getFullName() != null
                                ? r.getUser().getFullName() + " (" + r.getUser().getUsername() + ")"
                                : r.getUser().getUsername();
                        approvers.add(display);
                    } else if (r.getRole() != null) {
                        approvers.add("Anyone with role: " + r.getRole().getName());
                    }
                });

        return approvers;
    }

    private boolean matchesCreator(ApprovalRouting routing, String creatorUsername,
                                   Set<String> creatorRoles) {
        if (routing.getCreatorUser() != null) {
            return creatorUsername != null &&
                    routing.getCreatorUser().getUsername().equalsIgnoreCase(creatorUsername);
        }
        if (routing.getCreatorRole() != null) {
            return creatorRoles.contains(routing.getCreatorRole().getName());
        }
        // No creator filter — catch-all rule
        return true;
    }
}
