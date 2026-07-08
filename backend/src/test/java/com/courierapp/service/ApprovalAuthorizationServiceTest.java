package com.courierapp.service;

import com.courierapp.entity.ApprovalRouting;
import com.courierapp.entity.Permission;
import com.courierapp.entity.Role;
import com.courierapp.entity.User;
import com.courierapp.repository.ApprovalRoutingRepository;
import com.courierapp.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ApprovalAuthorizationService — unit tests")
class ApprovalAuthorizationServiceTest {

    @Mock ApprovalRoutingRepository approvalRoutingRepository;
    @Mock UserRepository userRepository;

    @InjectMocks
    ApprovalAuthorizationService service;

    private User approver;
    private User creator;
    private Role managerRole;
    private Permission adminViewPerm;

    @BeforeEach
    void setUp() {
        managerRole = new Role();
        managerRole.setId(1L);
        managerRole.setName("MANAGER");

        adminViewPerm = new Permission();
        adminViewPerm.setId(10L);
        adminViewPerm.setCode("ADMIN_VIEW");

        creator = new User();
        creator.setId(2L);
        creator.setUsername("priya");
        creator.setRoles(Set.of());

        approver = new User();
        approver.setId(3L);
        approver.setUsername("manager");
        approver.setRoles(Set.of(managerRole));
        managerRole.setPermissions(Set.of());
    }

    // ─── isAuthorizedApproverAtLevel ──────────────────────────────────────────

    @Nested
    @DisplayName("isAuthorizedApproverAtLevel()")
    class IsAuthorizedApproverAtLevel {

        @Test
        @DisplayName("returns false when approver user not found")
        void approverNotFound() {
            when(userRepository.findByUsername("ghost")).thenReturn(Optional.empty());
            assertThat(service.isAuthorizedApproverAtLevel("ghost", "priya", "BOOKING", 1)).isFalse();
        }

        @Test
        @DisplayName("admin bypass: ADMIN_VIEW grants approval at any level")
        void adminBypass() {
            Role adminRole = new Role();
            adminRole.setId(99L);
            adminRole.setName("ADMIN");
            adminRole.setPermissions(Set.of(adminViewPerm));
            approver.setRoles(Set.of(adminRole));

            when(userRepository.findByUsername("manager")).thenReturn(Optional.of(approver));

            assertThat(service.isAuthorizedApproverAtLevel("manager", "priya", "BOOKING", 1)).isTrue();
            // Should never need to check routing rules
            verify(approvalRoutingRepository, never()).findByActiveTrue();
        }

        @Test
        @DisplayName("returns true when approver matches by role in routing rule")
        void authorizedByRole() {
            ApprovalRouting routing = ApprovalRouting.builder()
                    .id(1L).module("BOOKING").level(1).active(true).role(managerRole).build();

            when(userRepository.findByUsername("manager")).thenReturn(Optional.of(approver));
            when(userRepository.findByUsername("priya")).thenReturn(Optional.of(creator));
            when(approvalRoutingRepository.findByActiveTrue()).thenReturn(List.of(routing));

            assertThat(service.isAuthorizedApproverAtLevel("manager", "priya", "BOOKING", 1)).isTrue();
        }

        @Test
        @DisplayName("returns true when approver matches by specific user in routing rule")
        void authorizedByUser() {
            ApprovalRouting routing = ApprovalRouting.builder()
                    .id(2L).module("BOOKING").level(1).active(true).user(approver).build();

            when(userRepository.findByUsername("manager")).thenReturn(Optional.of(approver));
            when(userRepository.findByUsername("priya")).thenReturn(Optional.of(creator));
            when(approvalRoutingRepository.findByActiveTrue()).thenReturn(List.of(routing));

            assertThat(service.isAuthorizedApproverAtLevel("manager", "priya", "BOOKING", 1)).isTrue();
        }

        @Test
        @DisplayName("returns false when approver role does not match any routing rule")
        void notAuthorized() {
            Role otherRole = new Role();
            otherRole.setId(50L);
            otherRole.setName("VIEWER");
            otherRole.setPermissions(Set.of());
            User viewer = new User();
            viewer.setId(7L);
            viewer.setUsername("bob");
            viewer.setRoles(Set.of(otherRole));

            ApprovalRouting routing = ApprovalRouting.builder()
                    .id(3L).module("BOOKING").level(1).active(true).role(managerRole).build();

            when(userRepository.findByUsername("bob")).thenReturn(Optional.of(viewer));
            when(userRepository.findByUsername("priya")).thenReturn(Optional.of(creator));
            when(approvalRoutingRepository.findByActiveTrue()).thenReturn(List.of(routing));

            assertThat(service.isAuthorizedApproverAtLevel("bob", "priya", "BOOKING", 1)).isFalse();
        }

        @Test
        @DisplayName("rule with creator-user filter only applies to that creator")
        void creatorUserFilterApplied() {
            User specificCreator = new User();
            specificCreator.setId(20L);
            specificCreator.setUsername("specificUser");

            // Rule only applies to bookings created by "specificUser"
            ApprovalRouting routing = ApprovalRouting.builder()
                    .id(4L).module("BOOKING").level(1).active(true)
                    .role(managerRole).creatorUser(specificCreator).build();

            when(userRepository.findByUsername("manager")).thenReturn(Optional.of(approver));
            when(userRepository.findByUsername("priya")).thenReturn(Optional.of(creator));
            when(approvalRoutingRepository.findByActiveTrue()).thenReturn(List.of(routing));

            // "priya" is NOT "specificUser" — rule should not apply
            assertThat(service.isAuthorizedApproverAtLevel("manager", "priya", "BOOKING", 1)).isFalse();
        }
    }

    // ─── getMaxLevel ──────────────────────────────────────────────────────────

    @Nested
    @DisplayName("getMaxLevel()")
    class GetMaxLevel {

        @Test
        @DisplayName("returns 1 when no routing rules exist")
        void noRulesDefaultsToOne() {
            when(userRepository.findByUsername("priya")).thenReturn(Optional.of(creator));
            when(approvalRoutingRepository.findByActiveTrue()).thenReturn(List.of());

            assertThat(service.getMaxLevel("priya", "BOOKING")).isEqualTo(1);
        }

        @Test
        @DisplayName("returns highest level across all matching rules")
        void returnsMaxLevel() {
            ApprovalRouting l1 = ApprovalRouting.builder().id(1L).module("BOOKING").level(1).active(true).role(managerRole).build();
            ApprovalRouting l2 = ApprovalRouting.builder().id(2L).module("BOOKING").level(2).active(true).role(managerRole).build();
            ApprovalRouting l3 = ApprovalRouting.builder().id(3L).module("BOOKING").level(3).active(true).role(managerRole).build();

            when(userRepository.findByUsername("priya")).thenReturn(Optional.of(creator));
            when(approvalRoutingRepository.findByActiveTrue()).thenReturn(List.of(l1, l2, l3));

            assertThat(service.getMaxLevel("priya", "BOOKING")).isEqualTo(3);
        }

        @Test
        @DisplayName("ignores rules for different modules")
        void ignoresOtherModules() {
            ApprovalRouting masterRule = ApprovalRouting.builder().id(5L).module("MASTER").level(4).active(true).role(managerRole).build();
            ApprovalRouting bookingRule = ApprovalRouting.builder().id(6L).module("BOOKING").level(2).active(true).role(managerRole).build();

            when(userRepository.findByUsername("priya")).thenReturn(Optional.of(creator));
            when(approvalRoutingRepository.findByActiveTrue()).thenReturn(List.of(masterRule, bookingRule));

            assertThat(service.getMaxLevel("priya", "BOOKING")).isEqualTo(2);
        }
    }

    // ─── isAdmin ──────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("isAdmin()")
    class IsAdmin {

        @Test
        @DisplayName("returns true when user has ADMIN_VIEW permission")
        void userIsAdmin() {
            Role adminRole = new Role();
            adminRole.setId(100L);
            adminRole.setName("ADMIN");
            adminRole.setPermissions(Set.of(adminViewPerm));
            approver.setRoles(Set.of(adminRole));

            when(userRepository.findByUsername("manager")).thenReturn(Optional.of(approver));

            assertThat(service.isAdmin("manager")).isTrue();
        }

        @Test
        @DisplayName("returns false when user does not have ADMIN_VIEW")
        void userNotAdmin() {
            managerRole.setPermissions(Set.of());
            when(userRepository.findByUsername("manager")).thenReturn(Optional.of(approver));

            assertThat(service.isAdmin("manager")).isFalse();
        }

        @Test
        @DisplayName("returns false when user not found")
        void userNotFound() {
            when(userRepository.findByUsername("ghost")).thenReturn(Optional.empty());
            assertThat(service.isAdmin("ghost")).isFalse();
        }
    }
}
