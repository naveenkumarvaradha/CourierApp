package com.courierapp.service;

import com.courierapp.dto.master.PartyResponse;
import com.courierapp.entity.Company;
import com.courierapp.entity.Party;
import com.courierapp.enums.PartyStatus;
import com.courierapp.enums.PartyType;
import com.courierapp.exception.BusinessException;
import com.courierapp.exception.ResourceNotFoundException;
import com.courierapp.mapper.PartyMapper;
import com.courierapp.repository.CompanyRepository;
import com.courierapp.repository.PartyRepository;
import com.courierapp.service.impl.PartyServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("PartyService — unit tests")
class PartyServiceTest {

    @Mock PartyRepository partyRepository;
    @Mock CompanyRepository companyRepository;
    @Mock PartyMapper partyMapper;
    @Mock ApprovalAuthorizationService approvalAuthorizationService;
    @Mock AuditLogService auditLogService;
    @Mock com.courierapp.kafka.CourierEventProducer eventProducer;
    @Mock com.courierapp.security.CurrentUserService currentUserService;

    @InjectMocks
    PartyServiceImpl partyService;

    private Party pendingParty;
    private PartyResponse dummyResponse;

    private static final Long COMPANY_ID = 1L;

    @BeforeEach
    void setUp() {
        lenient().when(currentUserService.requireCompanyId()).thenReturn(COMPANY_ID);
        Company company = Company.builder().id(COMPANY_ID).companyCode("1").name("Test Co").build();
        pendingParty = Party.builder()
                .id(5L)
                .partyCode("PTY000005")
                .partyName("Test Corp")
                .addressLine1("123 Main St")
                .city("Chennai")
                .state("TN")
                .pincode("600001")
                .country("India")
                .partyType(PartyType.SENDER)
                .partyStatus(PartyStatus.PENDING_APPROVAL)
                .active(false)
                .currentApprovalLevel(1)
                .company(company)
                .build();
        pendingParty.setCreatedBy("priya");
        pendingParty.setCreatedAt(Instant.now());

        dummyResponse = mock(PartyResponse.class);
    }

    // ─── delete ───────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("delete()")
    class Delete {

        @Test
        @DisplayName("throws when party is ACTIVE")
        void deleteActivePartyFails() {
            pendingParty.setPartyStatus(PartyStatus.ACTIVE);
            pendingParty.setActive(true);
            when(partyRepository.findById(5L)).thenReturn(Optional.of(pendingParty));

            assertThatThrownBy(() -> partyService.delete(5L))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("Cannot delete an ACTIVE party");
        }

        @Test
        @DisplayName("deletes PENDING_APPROVAL party successfully")
        void deletePendingSuccess() {
            when(partyRepository.findById(5L)).thenReturn(Optional.of(pendingParty));
            doNothing().when(partyRepository).delete(pendingParty);

            partyService.delete(5L);

            verify(partyRepository).delete(pendingParty);
        }

        @Test
        @DisplayName("throws ResourceNotFoundException for unknown party id")
        void deleteUnknown() {
            when(partyRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> partyService.delete(99L))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    // ─── approve ──────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("approve()")
    class Approve {

        @Test
        @DisplayName("escalates to next level when not at max")
        void approveEscalates() {
            when(partyRepository.findById(5L)).thenReturn(Optional.of(pendingParty));
            when(approvalAuthorizationService.isAuthorizedApproverAtLevel("mgr", "priya", "MASTER", 1)).thenReturn(true);
            when(approvalAuthorizationService.getMaxLevel("priya", "MASTER")).thenReturn(2);
            when(partyRepository.save(any())).thenReturn(pendingParty);
            when(partyMapper.toResponse(any())).thenReturn(dummyResponse);

            partyService.approve(5L, "mgr");

            assertThat(pendingParty.getCurrentApprovalLevel()).isEqualTo(2);
            assertThat(pendingParty.getPartyStatus()).isEqualTo(PartyStatus.PENDING_APPROVAL);
            assertThat(pendingParty.isActive()).isFalse();
        }

        @Test
        @DisplayName("activates party at final approval level")
        void approveFinalLevel() {
            when(partyRepository.findById(5L)).thenReturn(Optional.of(pendingParty));
            when(approvalAuthorizationService.isAuthorizedApproverAtLevel("mgr", "priya", "MASTER", 1)).thenReturn(true);
            when(approvalAuthorizationService.getMaxLevel("priya", "MASTER")).thenReturn(1);
            when(partyRepository.save(any())).thenReturn(pendingParty);
            when(partyMapper.toResponse(any())).thenReturn(dummyResponse);

            partyService.approve(5L, "mgr");

            assertThat(pendingParty.getPartyStatus()).isEqualTo(PartyStatus.ACTIVE);
            assertThat(pendingParty.isActive()).isTrue();
        }

        @Test
        @DisplayName("throws when approver is not authorized at level")
        void approveUnauthorized() {
            when(partyRepository.findById(5L)).thenReturn(Optional.of(pendingParty));
            when(approvalAuthorizationService.isAuthorizedApproverAtLevel("stranger", "priya", "MASTER", 1)).thenReturn(false);

            assertThatThrownBy(() -> partyService.approve(5L, "stranger"))
                    .isInstanceOf(BusinessException.class);
        }

        @Test
        @DisplayName("throws when party is not PENDING_APPROVAL")
        void approveWrongStatus() {
            pendingParty.setPartyStatus(PartyStatus.ACTIVE);
            when(partyRepository.findById(5L)).thenReturn(Optional.of(pendingParty));

            assertThatThrownBy(() -> partyService.approve(5L, "mgr"))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("PENDING_APPROVAL");
        }
    }

    // ─── reject ───────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("reject()")
    class Reject {

        @Test
        @DisplayName("sets status REJECTED and deactivates")
        void rejectSuccess() {
            when(partyRepository.findById(5L)).thenReturn(Optional.of(pendingParty));
            when(approvalAuthorizationService.isAuthorizedApproverAtLevel("mgr", "priya", "MASTER", 1)).thenReturn(true);
            when(partyRepository.save(any())).thenReturn(pendingParty);
            when(partyMapper.toResponse(any())).thenReturn(dummyResponse);

            partyService.reject(5L, "mgr", "Does not meet criteria");

            assertThat(pendingParty.getPartyStatus()).isEqualTo(PartyStatus.REJECTED);
            assertThat(pendingParty.isActive()).isFalse();
        }

        @Test
        @DisplayName("throws when party is not PENDING_APPROVAL")
        void rejectWrongStatus() {
            pendingParty.setPartyStatus(PartyStatus.REJECTED);
            when(partyRepository.findById(5L)).thenReturn(Optional.of(pendingParty));

            assertThatThrownBy(() -> partyService.reject(5L, "mgr", "reason"))
                    .isInstanceOf(BusinessException.class);
        }
    }
}
