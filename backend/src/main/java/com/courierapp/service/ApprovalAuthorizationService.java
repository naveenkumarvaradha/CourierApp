package com.courierapp.service;

import com.courierapp.entity.ApprovalRouting;
import com.courierapp.entity.User;
import com.courierapp.repository.ApprovalRoutingRepository;
import com.courierapp.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;
import java.util.stream.Collectors;

/**
 * Determines whether a given user is authorized to approve bookings, based on the
 * admin-configured approval routing (designated approver roles/users).
 */
@Service
public class ApprovalAuthorizationService {

    private final ApprovalRoutingRepository approvalRoutingRepository;
    private final UserRepository userRepository;

    public ApprovalAuthorizationService(ApprovalRoutingRepository approvalRoutingRepository,
                                        UserRepository userRepository) {
        this.approvalRoutingRepository = approvalRoutingRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public boolean isAuthorizedApprover(String username) {
        User user = userRepository.findByUsername(username).orElse(null);
        if (user == null) {
            return false;
        }
        Set<String> userRoleNames = user.getRoles().stream()
                .map(r -> r.getName()).collect(Collectors.toSet());

        for (ApprovalRouting routing : approvalRoutingRepository.findByActiveTrue()) {
            if (routing.getUser() != null
                    && routing.getUser().getUsername().equalsIgnoreCase(username)) {
                return true;
            }
            if (routing.getRole() != null
                    && userRoleNames.contains(routing.getRole().getName())) {
                return true;
            }
        }
        return false;
    }
}
