package com.courierapp.party.service;

import com.courierapp.party.entity.ApprovalRouting;
import com.courierapp.party.repository.ApprovalRoutingRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
public class ApprovalAuthorizationService {

    private final ApprovalRoutingRepository approvalRoutingRepository;

    public ApprovalAuthorizationService(ApprovalRoutingRepository approvalRoutingRepository) {
        this.approvalRoutingRepository = approvalRoutingRepository;
    }

    @Transactional(readOnly = true)
    public boolean isAuthorizedApproverAtLevel(String approverUsername, String creatorUsername,
                                               String module, int level) {
        List<ApprovalRouting> activeRoutes = approvalRoutingRepository.findByActiveTrue().stream()
                .filter(r -> module.equalsIgnoreCase(r.getModule()) && r.getLevel() == level)
                .toList();

        for (ApprovalRouting routing : activeRoutes) {
            if (!matchesCreator(routing, creatorUsername)) continue;
            if (routing.getApproverUsername() != null
                    && routing.getApproverUsername().equalsIgnoreCase(approverUsername)) {
                return true;
            }
            if (routing.getApproverRoleName() != null
                    && routing.getApproverRoleName().equalsIgnoreCase(approverUsername)) {
                return true;
            }
        }
        return false;
    }

    @Transactional(readOnly = true)
    public boolean isDesignatedApproverAtLevel(String approverUsername, String creatorUsername,
                                               String module, int level) {
        return isAuthorizedApproverAtLevel(approverUsername, creatorUsername, module, level);
    }

    @Transactional(readOnly = true)
    public int getMaxLevel(String creatorUsername, String module) {
        return approvalRoutingRepository.findByActiveTrue().stream()
                .filter(r -> module.equalsIgnoreCase(r.getModule()))
                .filter(r -> matchesCreator(r, creatorUsername))
                .mapToInt(ApprovalRouting::getLevel)
                .max()
                .orElse(1);
    }

    @Transactional(readOnly = true)
    public List<String> resolveApproversAtLevel(String creatorUsername, String module, int level) {
        List<String> approvers = new ArrayList<>();
        approvalRoutingRepository.findByActiveTrue().stream()
                .filter(r -> module.equalsIgnoreCase(r.getModule()) && r.getLevel() == level)
                .filter(r -> matchesCreator(r, creatorUsername))
                .forEach(r -> {
                    if (r.getApproverUsername() != null) {
                        approvers.add(r.getApproverUsername());
                    } else if (r.getApproverRoleName() != null) {
                        approvers.add("Anyone with role: " + r.getApproverRoleName());
                    }
                });
        return approvers;
    }

    private boolean matchesCreator(ApprovalRouting routing, String creatorUsername) {
        if (routing.getCreatorUsername() != null) {
            return creatorUsername != null &&
                    routing.getCreatorUsername().equalsIgnoreCase(creatorUsername);
        }
        return true;
    }
}
