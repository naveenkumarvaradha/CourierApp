package com.courierapp.service;

import com.courierapp.entity.ApprovalRouting;
import com.courierapp.entity.User;
import com.courierapp.repository.ApprovalRoutingRepository;
import com.courierapp.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class ApprovalAuthorizationService {

    private final ApprovalRoutingRepository approvalRoutingRepository;
    private final UserRepository userRepository;

    public ApprovalAuthorizationService(ApprovalRoutingRepository approvalRoutingRepository,
                                        UserRepository userRepository) {
        this.approvalRoutingRepository = approvalRoutingRepository;
        this.userRepository = userRepository;
    }

    /**
     * Returns true if {@code approverUsername} is a designated approver for a booking
     * that was created by {@code creatorUsername}.
     *
     * Routing rules that have a creatorRole set only fire when the booking creator
     * holds that role. Rules with no creatorRole are catch-all and fire for any creator.
     */
    @Transactional(readOnly = true)
    public boolean isAuthorizedApprover(String approverUsername, String creatorUsername) {
        User approver = userRepository.findByUsername(approverUsername).orElse(null);
        if (approver == null) return false;

        Set<String> approverRoles = approver.getRoles().stream()
                .map(r -> r.getName()).collect(Collectors.toSet());

        // Determine creator's roles (needed to match creator_role_id rules)
        Set<String> creatorRoles = Set.of();
        if (creatorUsername != null) {
            User creator = userRepository.findByUsername(creatorUsername).orElse(null);
            if (creator != null) {
                creatorRoles = creator.getRoles().stream()
                        .map(r -> r.getName()).collect(Collectors.toSet());
            }
        }
        final Set<String> resolvedCreatorRoles = creatorRoles;

        List<ApprovalRouting> activeRoutes = approvalRoutingRepository.findByActiveTrue();

        for (ApprovalRouting routing : activeRoutes) {
            // If scoped to a specific creator user, only apply when the creator matches exactly
            if (routing.getCreatorUser() != null) {
                if (creatorUsername == null ||
                        !routing.getCreatorUser().getUsername().equalsIgnoreCase(creatorUsername)) {
                    continue;
                }
            } else if (routing.getCreatorRole() != null) {
                // If scoped to a creator role, only apply when the creator holds that role
                if (!resolvedCreatorRoles.contains(routing.getCreatorRole().getName())) {
                    continue;
                }
            }

            // Check if the current approver matches this routing entry
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
}
