package com.courierapp.service;

import com.courierapp.entity.ApprovalRouting;
import com.courierapp.entity.User;
import com.courierapp.repository.ApprovalRoutingRepository;
import com.courierapp.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    /** Convenience overload for booking approvals (module = BOOKING). */
    @Transactional(readOnly = true)
    public boolean isAuthorizedApprover(String approverUsername, String creatorUsername) {
        return isAuthorizedApprover(approverUsername, creatorUsername, "BOOKING");
    }

    /**
     * Returns true if {@code approverUsername} is a designated approver for the given module
     * when the record was created by {@code creatorUsername}.
     *
     * Priority: creator-user rule > creator-role rule > catch-all.
     */
    @Transactional(readOnly = true)
    public boolean isAuthorizedApprover(String approverUsername, String creatorUsername, String module) {
        log.debug("Authorization check: approver={}, creator={}, module={}", approverUsername, creatorUsername, module);

        User approver = userRepository.findByUsername(approverUsername).orElse(null);
        if (approver == null) {
            log.warn("Approver '{}' not found in system", approverUsername);
            return false;
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
                .filter(r -> module.equalsIgnoreCase(r.getModule()))
                .toList();

        for (ApprovalRouting routing : activeRoutes) {
            if (routing.getCreatorUser() != null) {
                if (creatorUsername == null ||
                        !routing.getCreatorUser().getUsername().equalsIgnoreCase(creatorUsername)) {
                    continue;
                }
            } else if (routing.getCreatorRole() != null) {
                if (!resolvedCreatorRoles.contains(routing.getCreatorRole().getName())) {
                    continue;
                }
            }

            if (routing.getUser() != null
                    && routing.getUser().getUsername().equalsIgnoreCase(approverUsername)) {
                log.debug("Authorized via user-specific rule id={}", routing.getId());
                return true;
            }
            if (routing.getRole() != null
                    && approverRoles.contains(routing.getRole().getName())) {
                log.debug("Authorized via role rule id={}, role={}", routing.getId(), routing.getRole().getName());
                return true;
            }
        }

        log.warn("Authorization DENIED: approver='{}' is not a designated approver for module={} created by '{}'",
                approverUsername, module, creatorUsername);
        return false;
    }
}
