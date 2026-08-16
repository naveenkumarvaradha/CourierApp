package com.courierapp.service;

import com.courierapp.dto.booking.ApprovalDecisionRequest;
import com.courierapp.dto.dc.DcRequest;
import com.courierapp.dto.dc.DcResponse;
import com.courierapp.entity.Company;
import com.courierapp.entity.CourierWay;
import com.courierapp.entity.DeliveryChallan;
import com.courierapp.entity.Party;
import com.courierapp.entity.Unit;
import com.courierapp.enums.CourierMode;
import com.courierapp.enums.DcStatus;
import com.courierapp.enums.DcType;
import com.courierapp.enums.PartyType;
import com.courierapp.enums.ReceiverType;
import com.courierapp.exception.BusinessException;
import com.courierapp.exception.ResourceNotFoundException;
import com.courierapp.mapper.DcMapper;
import com.courierapp.repository.*;
import com.courierapp.service.impl.DeliveryChallanServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("DeliveryChallanService — unit tests")
class DeliveryChallanServiceTest {

    @Mock DeliveryChallanRepository deliveryChallanRepository;
    @Mock UnitRepository unitRepository;
    @Mock PartyRepository partyRepository;
    @Mock CourierWayRepository courierWayRepository;
    @Mock PackageTypeRepository packageTypeRepository;
    @Mock CompanySettingsRepository companySettingsRepository;
    @Mock DcNumberGenerator dcNumberGenerator;
    @Mock DcMapper dcMapper;
    @Mock DcPdfService dcPdfService;
    @Mock AuditLogService auditLogService;
    @Mock ApprovalAuthorizationService approvalAuthorizationService;
    @Mock com.courierapp.security.CurrentUserService currentUserService;

    @InjectMocks
    DeliveryChallanServiceImpl deliveryChallanService;

    private Unit senderUnit;
    private Unit otherUnit;
    private Party receiverParty;
    private CourierWay activeWay;
    private DeliveryChallan pendingDc;
    private DcResponse dummyResponse;

    private static final Long COMPANY_ID = 1L;

    @BeforeEach
    void setUp() {
        lenient().when(currentUserService.requireCompanyId()).thenReturn(COMPANY_ID);
        Company company = Company.builder().id(COMPANY_ID).companyCode("1").name("Test Co").build();
        senderUnit = Unit.builder().id(1L).unitName("Coimbatore Factory").active(true).company(company).build();
        otherUnit = Unit.builder().id(2L).unitName("Warehouse").active(true).company(company).build();
        receiverParty = Party.builder().id(5L).partyCode("PTY000005").partyName("Avery")
                .partyType(PartyType.RECEIVER).active(true).build();
        activeWay = CourierWay.builder().id(3L).name("DHL").active(true).build();

        pendingDc = DeliveryChallan.builder()
                .id(20L)
                .dcNumber("C1-DC-2026-00001")
                .dcType(DcType.RETURNABLE)
                .unit(senderUnit)
                .receiverType(ReceiverType.PARTY)
                .receiverParty(receiverParty)
                .itemDescription("Test goods")
                .weightKg(BigDecimal.valueOf(5))
                .noOfPackages(1)
                .courierMode(CourierMode.SURFACE)
                .courierWay(activeWay)
                .status(DcStatus.PENDING_APPROVAL)
                .currentApprovalLevel(1)
                .build();
        pendingDc.setCreatedBy("alice");

        dummyResponse = mock(DcResponse.class);
    }

    private DcRequest partyReceiverRequest(Long unitId, Long receiverPartyId) {
        return new DcRequest(unitId, DcType.RETURNABLE, ReceiverType.PARTY, receiverPartyId, null,
                3L, null, "Test goods", BigDecimal.valueOf(5), 1, CourierMode.SURFACE, null, null, null);
    }

    private DcRequest unitReceiverRequest(Long unitId, Long receiverUnitId) {
        return new DcRequest(unitId, DcType.RETURNABLE, ReceiverType.UNIT, null, receiverUnitId,
                3L, null, "Test goods", BigDecimal.valueOf(5), 1, CourierMode.SURFACE, null, null, null);
    }

    // ─── create ───────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("create()")
    class Create {

        @Test
        @DisplayName("creates a DRAFT DC with an external party receiver")
        void createWithPartyReceiver() {
            when(unitRepository.findById(1L)).thenReturn(Optional.of(senderUnit));
            when(partyRepository.findById(5L)).thenReturn(Optional.of(receiverParty));
            when(courierWayRepository.findById(3L)).thenReturn(Optional.of(activeWay));
            when(deliveryChallanRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(dcNumberGenerator.next(any(), any())).thenReturn("C1-DC-2026-00002");
            when(dcMapper.toResponse(any())).thenReturn(dummyResponse);

            DcResponse result = deliveryChallanService.create(partyReceiverRequest(1L, 5L));

            assertThat(result).isSameAs(dummyResponse);
            verify(deliveryChallanRepository).save(argThat(dc ->
                    dc.getStatus() == DcStatus.DRAFT
                            && dc.getReceiverParty() == receiverParty
                            && dc.getReceiverUnit() == null));
        }

        @Test
        @DisplayName("creates a DRAFT DC with a company-unit receiver (inter-branch transfer)")
        void createWithUnitReceiver() {
            when(unitRepository.findById(1L)).thenReturn(Optional.of(senderUnit));
            when(unitRepository.findById(2L)).thenReturn(Optional.of(otherUnit));
            when(courierWayRepository.findById(3L)).thenReturn(Optional.of(activeWay));
            when(deliveryChallanRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(dcNumberGenerator.next(any(), any())).thenReturn("C1-DC-2026-00003");
            when(dcMapper.toResponse(any())).thenReturn(dummyResponse);

            deliveryChallanService.create(unitReceiverRequest(1L, 2L));

            verify(deliveryChallanRepository).save(argThat(dc ->
                    dc.getReceiverUnit() == otherUnit && dc.getReceiverParty() == null));
        }

        @Test
        @DisplayName("rejects a receiver unit that is the same as the sending unit")
        void rejectsSameUnitAsReceiver() {
            when(unitRepository.findById(1L)).thenReturn(Optional.of(senderUnit));

            assertThatThrownBy(() -> deliveryChallanService.create(unitReceiverRequest(1L, 1L)))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("cannot be the same");
        }

        @Test
        @DisplayName("rejects PARTY receiver type with no receiverPartyId")
        void rejectsMissingReceiverParty() {
            when(unitRepository.findById(1L)).thenReturn(Optional.of(senderUnit));

            assertThatThrownBy(() -> deliveryChallanService.create(partyReceiverRequest(1L, null)))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("Receiver party is required");
        }

        @Test
        @DisplayName("rejects UNIT receiver type with no receiverUnitId")
        void rejectsMissingReceiverUnit() {
            when(unitRepository.findById(1L)).thenReturn(Optional.of(senderUnit));

            assertThatThrownBy(() -> deliveryChallanService.create(unitReceiverRequest(1L, null)))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("Receiver unit is required");
        }

        @Test
        @DisplayName("rejects an inactive courier way")
        void rejectsInactiveCourierWay() {
            CourierWay inactiveWay = CourierWay.builder().id(3L).name("Old Way").active(false).build();
            when(unitRepository.findById(1L)).thenReturn(Optional.of(senderUnit));
            when(partyRepository.findById(5L)).thenReturn(Optional.of(receiverParty));
            when(courierWayRepository.findById(3L)).thenReturn(Optional.of(inactiveWay));

            assertThatThrownBy(() -> deliveryChallanService.create(partyReceiverRequest(1L, 5L)))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("not active");
        }

        @Test
        @DisplayName("throws ResourceNotFoundException for an unknown sending unit")
        void unknownUnit() {
            when(unitRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> deliveryChallanService.create(partyReceiverRequest(99L, 5L)))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    // ─── update ───────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("update()")
    class Update {

        @Test
        @DisplayName("throws when DC is APPROVED (past the editable window)")
        void rejectsEditingApproved() {
            pendingDc.setStatus(DcStatus.APPROVED);
            when(deliveryChallanRepository.findById(20L)).thenReturn(Optional.of(pendingDc));

            assertThatThrownBy(() -> deliveryChallanService.update(20L, partyReceiverRequest(1L, 5L)))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("DRAFT or PENDING_APPROVAL");
        }

        @Test
        @DisplayName("allows editing while PENDING_APPROVAL")
        void allowsEditingPendingApproval() {
            when(deliveryChallanRepository.findById(20L)).thenReturn(Optional.of(pendingDc));
            when(unitRepository.findById(1L)).thenReturn(Optional.of(senderUnit));
            when(partyRepository.findById(5L)).thenReturn(Optional.of(receiverParty));
            when(courierWayRepository.findById(3L)).thenReturn(Optional.of(activeWay));
            when(deliveryChallanRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(dcMapper.toResponse(any())).thenReturn(dummyResponse);

            deliveryChallanService.update(20L, partyReceiverRequest(1L, 5L));

            verify(deliveryChallanRepository).save(pendingDc);
        }
    }

    // ─── changeStatus ─────────────────────────────────────────────────────────

    @Nested
    @DisplayName("changeStatus()")
    class ChangeStatus {

        @Test
        @DisplayName("APPROVED -> ISSUED is valid")
        void approvedToIssued() {
            pendingDc.setStatus(DcStatus.APPROVED);
            when(deliveryChallanRepository.findById(20L)).thenReturn(Optional.of(pendingDc));
            when(deliveryChallanRepository.save(any())).thenReturn(pendingDc);
            when(dcMapper.toResponse(any())).thenReturn(dummyResponse);

            deliveryChallanService.changeStatus(20L, DcStatus.ISSUED);

            assertThat(pendingDc.getStatus()).isEqualTo(DcStatus.ISSUED);
        }

        @Test
        @DisplayName("ISSUED -> DELIVERED is valid")
        void issuedToDelivered() {
            pendingDc.setStatus(DcStatus.ISSUED);
            when(deliveryChallanRepository.findById(20L)).thenReturn(Optional.of(pendingDc));
            when(deliveryChallanRepository.save(any())).thenReturn(pendingDc);
            when(dcMapper.toResponse(any())).thenReturn(dummyResponse);

            deliveryChallanService.changeStatus(20L, DcStatus.DELIVERED);

            assertThat(pendingDc.getStatus()).isEqualTo(DcStatus.DELIVERED);
        }

        @Test
        @DisplayName("DRAFT -> ISSUED is invalid")
        void draftToIssuedInvalid() {
            pendingDc.setStatus(DcStatus.DRAFT);
            when(deliveryChallanRepository.findById(20L)).thenReturn(Optional.of(pendingDc));

            assertThatThrownBy(() -> deliveryChallanService.changeStatus(20L, DcStatus.ISSUED))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("Cannot move");
        }

        @Test
        @DisplayName("DELIVERED -> RETURNED is invalid — RETURNED is only reachable via a DC Receipt")
        void deliveredToReturnedInvalid() {
            pendingDc.setStatus(DcStatus.DELIVERED);
            when(deliveryChallanRepository.findById(20L)).thenReturn(Optional.of(pendingDc));

            assertThatThrownBy(() -> deliveryChallanService.changeStatus(20L, DcStatus.RETURNED))
                    .isInstanceOf(BusinessException.class);
        }
    }

    // ─── submitForApproval ────────────────────────────────────────────────────

    @Nested
    @DisplayName("submitForApproval()")
    class SubmitForApproval {

        @Test
        @DisplayName("transitions DRAFT -> PENDING_APPROVAL at level 1")
        void submitSuccess() {
            pendingDc.setStatus(DcStatus.DRAFT);
            when(deliveryChallanRepository.findById(20L)).thenReturn(Optional.of(pendingDc));
            when(deliveryChallanRepository.save(any())).thenReturn(pendingDc);
            when(dcMapper.toResponse(any())).thenReturn(dummyResponse);

            deliveryChallanService.submitForApproval(20L);

            assertThat(pendingDc.getStatus()).isEqualTo(DcStatus.PENDING_APPROVAL);
            assertThat(pendingDc.getCurrentApprovalLevel()).isEqualTo(1);
        }

        @Test
        @DisplayName("throws if DC is not DRAFT")
        void submitNonDraft() {
            pendingDc.setStatus(DcStatus.PENDING_APPROVAL);
            when(deliveryChallanRepository.findById(20L)).thenReturn(Optional.of(pendingDc));

            assertThatThrownBy(() -> deliveryChallanService.submitForApproval(20L))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("DRAFT");
        }
    }

    // ─── approve ──────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("approve()")
    class Approve {

        @Test
        @DisplayName("escalates to next level when not at max level")
        void approveEscalates() {
            when(deliveryChallanRepository.findById(20L)).thenReturn(Optional.of(pendingDc));
            when(approvalAuthorizationService.isAuthorizedApproverAtLevel("approver", "alice", "DELIVERY_CHALLAN", 1))
                    .thenReturn(true);
            when(approvalAuthorizationService.getMaxLevel("alice", "DELIVERY_CHALLAN")).thenReturn(2);
            when(deliveryChallanRepository.save(any())).thenReturn(pendingDc);
            when(dcMapper.toResponse(any())).thenReturn(dummyResponse);

            deliveryChallanService.approve(20L, new ApprovalDecisionRequest("LGTM"), "approver");

            assertThat(pendingDc.getCurrentApprovalLevel()).isEqualTo(2);
            assertThat(pendingDc.getStatus()).isEqualTo(DcStatus.PENDING_APPROVAL);
        }

        @Test
        @DisplayName("sets status APPROVED at final level")
        void approveFinalLevel() {
            when(deliveryChallanRepository.findById(20L)).thenReturn(Optional.of(pendingDc));
            when(approvalAuthorizationService.isAuthorizedApproverAtLevel("approver", "alice", "DELIVERY_CHALLAN", 1))
                    .thenReturn(true);
            when(approvalAuthorizationService.getMaxLevel("alice", "DELIVERY_CHALLAN")).thenReturn(1);
            when(deliveryChallanRepository.save(any())).thenReturn(pendingDc);
            when(dcMapper.toResponse(any())).thenReturn(dummyResponse);

            deliveryChallanService.approve(20L, new ApprovalDecisionRequest("Approved"), "approver");

            assertThat(pendingDc.getStatus()).isEqualTo(DcStatus.APPROVED);
        }

        @Test
        @DisplayName("throws if approver is not authorized at the current level")
        void approveUnauthorized() {
            when(deliveryChallanRepository.findById(20L)).thenReturn(Optional.of(pendingDc));
            when(approvalAuthorizationService.isAuthorizedApproverAtLevel("hacker", "alice", "DELIVERY_CHALLAN", 1))
                    .thenReturn(false);

            assertThatThrownBy(() -> deliveryChallanService.approve(20L, null, "hacker"))
                    .isInstanceOf(BusinessException.class);
        }

        @Test
        @DisplayName("throws if DC is not PENDING_APPROVAL")
        void approveWrongStatus() {
            pendingDc.setStatus(DcStatus.DRAFT);
            when(deliveryChallanRepository.findById(20L)).thenReturn(Optional.of(pendingDc));

            assertThatThrownBy(() -> deliveryChallanService.approve(20L, null, "approver"))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("PENDING_APPROVAL");
        }
    }

    // ─── reject ───────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("reject()")
    class Reject {

        @Test
        @DisplayName("sets status REJECTED")
        void rejectSuccess() {
            when(deliveryChallanRepository.findById(20L)).thenReturn(Optional.of(pendingDc));
            when(approvalAuthorizationService.isAuthorizedApproverAtLevel("approver", "alice", "DELIVERY_CHALLAN", 1))
                    .thenReturn(true);
            when(deliveryChallanRepository.save(any())).thenReturn(pendingDc);
            when(dcMapper.toResponse(any())).thenReturn(dummyResponse);

            deliveryChallanService.reject(20L, new ApprovalDecisionRequest("Not valid"), "approver");

            assertThat(pendingDc.getStatus()).isEqualTo(DcStatus.REJECTED);
        }
    }
}
