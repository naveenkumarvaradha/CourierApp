package com.courierapp.service;

import com.courierapp.dto.booking.ApprovalDecisionRequest;
import com.courierapp.dto.booking.BookingResponse;
import com.courierapp.dto.booking.StatusUpdateRequest;
import com.courierapp.entity.Booking;
import com.courierapp.entity.Company;
import com.courierapp.entity.Party;
import com.courierapp.enums.BookingStatus;
import com.courierapp.enums.CourierMode;
import com.courierapp.enums.PartyType;
import com.courierapp.exception.BusinessException;
import com.courierapp.exception.ResourceNotFoundException;
import com.courierapp.mapper.BookingMapper;
import com.courierapp.repository.*;
import com.courierapp.service.impl.BookingServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("BookingService — unit tests")
class BookingServiceTest {

    @Mock BookingRepository bookingRepository;
    @Mock PartyRepository partyRepository;
    @Mock CompanySettingsRepository companySettingsRepository;
    @Mock CourierWayRepository courierWayRepository;
    @Mock PackageTypeRepository packageTypeRepository;
    @Mock UserRepository userRepository;
    @Mock BookingNumberGenerator bookingNumberGenerator;
    @Mock BookingMapper bookingMapper;
    @Mock StickerPdfService stickerPdfService;
    @Mock ApprovalAuthorizationService approvalAuthorizationService;
    @Mock AuditLogService auditLogService;
    @Mock com.courierapp.repository.StickerFieldConfigRepository stickerFieldConfigRepository;
    @Mock com.courierapp.kafka.CourierEventProducer eventProducer;
    @Mock com.courierapp.security.CurrentUserService currentUserService;

    @InjectMocks
    BookingServiceImpl bookingService;

    private Booking pendingBooking;
    private BookingResponse dummyResponse;

    private static final Long COMPANY_ID = 1L;

    @BeforeEach
    void setUp() {
        lenient().when(currentUserService.requireCompanyId()).thenReturn(COMPANY_ID);
        Company company = Company.builder().id(COMPANY_ID).companyCode("1").name("Test Co").build();
        Party sender = Party.builder().id(1L).partyCode("PTY001").partyName("Sender Co")
                .partyType(PartyType.SENDER).active(true).city("Chennai").company(company).build();
        Party receiver = Party.builder().id(2L).partyCode("PTY002").partyName("Receiver Co")
                .partyType(PartyType.RECEIVER).active(true).city("Mumbai").build();

        pendingBooking = Booking.builder()
                .id(10L)
                .bookingNumber("CB-2024-0001")
                .bookingDate(LocalDate.now())
                .sender(sender)
                .receiver(receiver)
                .status(BookingStatus.PENDING_APPROVAL)
                .currentApprovalLevel(1)
                .itemDescription("Electronics")
                .weightKg(BigDecimal.valueOf(2.5))
                .noOfPackages(1)
                .courierMode(CourierMode.AIR)
                .build();
        pendingBooking.setCreatedBy("alice");

        dummyResponse = mock(BookingResponse.class);
    }

    // ─── delete ───────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("delete()")
    class Delete {

        @Test
        @DisplayName("throws when booking is not in BOOKED state")
        void deleteRejectsNonBooked() {
            pendingBooking.setStatus(BookingStatus.PENDING_APPROVAL);
            when(bookingRepository.findById(10L)).thenReturn(Optional.of(pendingBooking));

            assertThatThrownBy(() -> bookingService.delete(10L))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("BOOKED");
        }

        @Test
        @DisplayName("throws ResourceNotFoundException for unknown id")
        void deleteUnknownId() {
            when(bookingRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> bookingService.delete(99L))
                    .isInstanceOf(ResourceNotFoundException.class);
        }

        @Test
        @DisplayName("deletes a BOOKED booking successfully")
        void deleteBookedSuccess() {
            pendingBooking.setStatus(BookingStatus.BOOKED);
            when(bookingRepository.findById(10L)).thenReturn(Optional.of(pendingBooking));
            doNothing().when(bookingRepository).delete(pendingBooking);

            bookingService.delete(10L);

            verify(bookingRepository).delete(pendingBooking);
        }
    }

    // ─── submitForApproval ────────────────────────────────────────────────────

    @Nested
    @DisplayName("submitForApproval()")
    class SubmitForApproval {

        @Test
        @DisplayName("transitions BOOKED → PENDING_APPROVAL at level 1")
        void submitSuccess() {
            pendingBooking.setStatus(BookingStatus.BOOKED);
            when(bookingRepository.findById(10L)).thenReturn(Optional.of(pendingBooking));
            when(bookingRepository.save(any())).thenReturn(pendingBooking);
            when(bookingMapper.toResponse(any())).thenReturn(dummyResponse);

            bookingService.submitForApproval(10L);

            assertThat(pendingBooking.getStatus()).isEqualTo(BookingStatus.PENDING_APPROVAL);
            assertThat(pendingBooking.getCurrentApprovalLevel()).isEqualTo(1);
        }

        @Test
        @DisplayName("throws if booking is already PENDING_APPROVAL")
        void submitAlreadyPending() {
            pendingBooking.setStatus(BookingStatus.PENDING_APPROVAL);
            when(bookingRepository.findById(10L)).thenReturn(Optional.of(pendingBooking));

            assertThatThrownBy(() -> bookingService.submitForApproval(10L))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("BOOKED");
        }
    }

    // ─── approve ──────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("approve()")
    class Approve {

        @Test
        @DisplayName("escalates to next level when not at max level")
        void approveEscalates() {
            when(bookingRepository.findById(10L)).thenReturn(Optional.of(pendingBooking));
            when(approvalAuthorizationService.isAuthorizedApproverAtLevel("approver", "alice", "BOOKING", 1)).thenReturn(true);
            when(approvalAuthorizationService.getMaxLevel("alice", "BOOKING")).thenReturn(2);
            when(bookingRepository.save(any())).thenReturn(pendingBooking);
            when(bookingMapper.toResponse(any())).thenReturn(dummyResponse);

            bookingService.approve(10L, new ApprovalDecisionRequest("LGTM"), "approver");

            assertThat(pendingBooking.getCurrentApprovalLevel()).isEqualTo(2);
            assertThat(pendingBooking.getStatus()).isEqualTo(BookingStatus.PENDING_APPROVAL);
        }

        @Test
        @DisplayName("sets status APPROVED at final level")
        void approveFinalLevel() {
            when(bookingRepository.findById(10L)).thenReturn(Optional.of(pendingBooking));
            when(approvalAuthorizationService.isAuthorizedApproverAtLevel("approver", "alice", "BOOKING", 1)).thenReturn(true);
            when(approvalAuthorizationService.getMaxLevel("alice", "BOOKING")).thenReturn(1);
            when(bookingRepository.save(any())).thenReturn(pendingBooking);
            when(bookingMapper.toResponse(any())).thenReturn(dummyResponse);

            bookingService.approve(10L, new ApprovalDecisionRequest("Approved"), "approver");

            assertThat(pendingBooking.getStatus()).isEqualTo(BookingStatus.APPROVED);
        }

        @Test
        @DisplayName("throws if approver is not authorized")
        void approveUnauthorized() {
            when(bookingRepository.findById(10L)).thenReturn(Optional.of(pendingBooking));
            when(approvalAuthorizationService.isAuthorizedApproverAtLevel("hacker", "alice", "BOOKING", 1)).thenReturn(false);

            assertThatThrownBy(() -> bookingService.approve(10L, null, "hacker"))
                    .isInstanceOf(BusinessException.class);
        }

        @Test
        @DisplayName("throws if booking is not PENDING_APPROVAL")
        void approveWrongStatus() {
            pendingBooking.setStatus(BookingStatus.BOOKED);
            when(bookingRepository.findById(10L)).thenReturn(Optional.of(pendingBooking));

            assertThatThrownBy(() -> bookingService.approve(10L, null, "approver"))
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
            when(bookingRepository.findById(10L)).thenReturn(Optional.of(pendingBooking));
            when(approvalAuthorizationService.isAuthorizedApproverAtLevel("approver", "alice", "BOOKING", 1)).thenReturn(true);
            when(bookingRepository.save(any())).thenReturn(pendingBooking);
            when(bookingMapper.toResponse(any())).thenReturn(dummyResponse);

            bookingService.reject(10L, new ApprovalDecisionRequest("Not valid"), "approver");

            assertThat(pendingBooking.getStatus()).isEqualTo(BookingStatus.REJECTED);
        }
    }

    // ─── changeStatus ─────────────────────────────────────────────────────────

    @Nested
    @DisplayName("changeStatus()")
    class ChangeStatus {

        @Test
        @DisplayName("APPROVED → IN_TRANSIT is valid")
        void validTransition() {
            pendingBooking.setStatus(BookingStatus.APPROVED);
            when(bookingRepository.findById(10L)).thenReturn(Optional.of(pendingBooking));
            when(bookingRepository.save(any())).thenReturn(pendingBooking);
            when(bookingMapper.toResponse(any())).thenReturn(dummyResponse);

            bookingService.changeStatus(10L, new StatusUpdateRequest(BookingStatus.IN_TRANSIT));

            assertThat(pendingBooking.getStatus()).isEqualTo(BookingStatus.IN_TRANSIT);
        }

        @Test
        @DisplayName("BOOKED → DELIVERED is invalid")
        void invalidTransition() {
            pendingBooking.setStatus(BookingStatus.BOOKED);
            when(bookingRepository.findById(10L)).thenReturn(Optional.of(pendingBooking));

            assertThatThrownBy(() -> bookingService.changeStatus(10L,
                    new StatusUpdateRequest(BookingStatus.DELIVERED)))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("Cannot transition");
        }

        @Test
        @DisplayName("IN_TRANSIT → CANCELLED is valid")
        void inTransitCancel() {
            pendingBooking.setStatus(BookingStatus.IN_TRANSIT);
            when(bookingRepository.findById(10L)).thenReturn(Optional.of(pendingBooking));
            when(bookingRepository.save(any())).thenReturn(pendingBooking);
            when(bookingMapper.toResponse(any())).thenReturn(dummyResponse);

            bookingService.changeStatus(10L, new StatusUpdateRequest(BookingStatus.CANCELLED));

            assertThat(pendingBooking.getStatus()).isEqualTo(BookingStatus.CANCELLED);
        }
    }

    // ─── isCreatorOf ──────────────────────────────────────────────────────────

    @Nested
    @DisplayName("isCreatorOf()")
    class IsCreatorOf {

        @Test
        @DisplayName("returns true when username matches createdBy")
        void creatorMatch() {
            when(bookingRepository.findById(10L)).thenReturn(Optional.of(pendingBooking));
            assertThat(bookingService.isCreatorOf(10L, "alice")).isTrue();
        }

        @Test
        @DisplayName("returns false when username does not match")
        void creatorMismatch() {
            when(bookingRepository.findById(10L)).thenReturn(Optional.of(pendingBooking));
            assertThat(bookingService.isCreatorOf(10L, "bob")).isFalse();
        }

        @Test
        @DisplayName("returns false when booking not found")
        void creatorNotFound() {
            when(bookingRepository.findById(99L)).thenReturn(Optional.empty());
            assertThat(bookingService.isCreatorOf(99L, "alice")).isFalse();
        }
    }
}
