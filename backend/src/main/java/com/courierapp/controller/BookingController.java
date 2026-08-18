package com.courierapp.controller;

import com.courierapp.dto.PageResponse;
import com.courierapp.dto.admin.CompanySettingsResponse;
import com.courierapp.dto.approval.ApprovalInfoResponse;
import com.courierapp.dto.booking.ApprovalDecisionRequest;
import com.courierapp.dto.booking.AwbUpdateRequest;
import com.courierapp.dto.booking.BookingRequest;
import com.courierapp.dto.booking.BookingResponse;
import com.courierapp.dto.booking.StatusUpdateRequest;
import com.courierapp.entity.Booking;
import com.courierapp.enums.BookingStatus;
import com.courierapp.enums.CourierMode;
import com.courierapp.repository.BookingRepository;
import com.courierapp.repository.UserRepository;
import com.courierapp.service.AdminService;
import com.courierapp.service.ApprovalAuthorizationService;
import com.courierapp.service.BookingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@Slf4j
@RestController
@RequestMapping("/bookings")
@Tag(name = "Courier Bookings")
public class BookingController {

    private final BookingService bookingService;
    private final BookingRepository bookingRepository;
    private final ApprovalAuthorizationService approvalAuthorizationService;
    private final UserRepository userRepository;
    private final AdminService adminService;
    private final com.courierapp.service.tracking.TrackingService trackingService;

    public BookingController(BookingService bookingService,
                             BookingRepository bookingRepository,
                             ApprovalAuthorizationService approvalAuthorizationService,
                             UserRepository userRepository,
                             AdminService adminService,
                             com.courierapp.service.tracking.TrackingService trackingService) {
        this.bookingService = bookingService;
        this.bookingRepository = bookingRepository;
        this.approvalAuthorizationService = approvalAuthorizationService;
        this.userRepository = userRepository;
        this.adminService = adminService;
        this.trackingService = trackingService;
    }

    @GetMapping("/{id}/tracking")
    @PreAuthorize("hasAuthority('BOOKING_VIEW')")
    @Operation(summary = "Fetch live tracking events from the carrier (DHL/Maruti) and return full history")
    public java.util.List<com.courierapp.dto.tracking.TrackingEventResponse> tracking(@PathVariable Long id) {
        return trackingService.trackAndSync(id);
    }

    @GetMapping
    @PreAuthorize("hasAuthority('BOOKING_VIEW')")
    public PageResponse<BookingResponse> search(
            @RequestParam(required = false) String bookingNumber,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            @RequestParam(required = false) BookingStatus status,
            @RequestParam(required = false) Long senderId,
            @RequestParam(required = false) Long receiverId,
            @RequestParam(required = false) CourierMode mode,
            @RequestParam(required = false) String receiverName,
            @RequestParam(required = false) String receiverCompanyName,
            @PageableDefault(size = 20, sort = "id") Pageable pageable) {
        return bookingService.search(bookingNumber, fromDate, toDate, status, senderId, receiverId,
                mode, receiverName, receiverCompanyName, pageable);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('BOOKING_VIEW')")
    public BookingResponse get(@PathVariable Long id) {
        return bookingService.get(id);
    }

    @GetMapping("/{id}/approval-info")
    @PreAuthorize("hasAuthority('BOOKING_VIEW')")
    public ApprovalInfoResponse approvalInfo(@PathVariable Long id) {
        Booking b = bookingRepository.findById(id)
                .orElseThrow(() -> new com.courierapp.exception.ResourceNotFoundException("Booking", id));
        String creator = b.getCreatedBy();
        int currentLevel = b.getCurrentApprovalLevel();
        int maxLevel = approvalAuthorizationService.getMaxLevel(creator, "BOOKING");
        java.util.List<String> approvers = approvalAuthorizationService
                .resolveApproversAtLevel(creator, "BOOKING", currentLevel);
        String summary = "Level " + currentLevel + " of " + maxLevel;
        return new ApprovalInfoResponse(currentLevel, maxLevel, approvers, summary);
    }

    @PostMapping
    @PreAuthorize("hasAuthority('BOOKING_CREATE')")
    @ResponseStatus(HttpStatus.CREATED)
    public BookingResponse create(@Valid @RequestBody BookingRequest request) {
        return bookingService.create(request);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('BOOKING_UPDATE')")
    public BookingResponse update(@PathVariable Long id, @Valid @RequestBody BookingRequest request) {
        return bookingService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('BOOKING_DELETE')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        bookingService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/submit")
    @PreAuthorize("hasAuthority('BOOKING_UPDATE')")
    public BookingResponse submit(@PathVariable Long id) {
        return bookingService.submitForApproval(id);
    }

    @PostMapping("/{id}/approve")
    @PreAuthorize("hasAuthority('BOOKING_APPROVE')")
    public BookingResponse approve(@PathVariable Long id,
                                   @Valid @RequestBody(required = false) ApprovalDecisionRequest request,
                                   Authentication authentication) {
        return bookingService.approve(id, request, authentication.getName());
    }

    @PostMapping("/{id}/reject")
    @PreAuthorize("hasAuthority('BOOKING_APPROVE')")
    public BookingResponse reject(@PathVariable Long id,
                                  @Valid @RequestBody(required = false) ApprovalDecisionRequest request,
                                  Authentication authentication) {
        return bookingService.reject(id, request, authentication.getName());
    }

    @PostMapping("/{id}/status")
    @PreAuthorize("hasAuthority('BOOKING_UPDATE')")
    public BookingResponse changeStatus(@PathVariable Long id,
                                        @Valid @RequestBody StatusUpdateRequest request) {
        return bookingService.changeStatus(id, request);
    }

    @PutMapping("/{id}/awb")
    @PreAuthorize("hasAuthority('BOOKING_UPDATE') or hasAuthority('BOOKING_PRINT') or @bookingService.isCreatorOf(#id, authentication.name)")
    public BookingResponse updateAwb(@PathVariable Long id,
                                     @Valid @RequestBody AwbUpdateRequest request) {
        return bookingService.updateAwb(id, request);
    }

    @GetMapping(value = "/{id}/sticker", produces = MediaType.APPLICATION_PDF_VALUE)
    @PreAuthorize("hasAuthority('BOOKING_PRINT') or @bookingService.isCreatorOf(#id, authentication.name)")
    @Operation(summary = "Generate landscape 150×110mm shipping label PDF and mark print as taken")
    public ResponseEntity<byte[]> sticker(@PathVariable Long id) {
        byte[] pdf = bookingService.generateStickerPdf(id);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=sticker-" + id + ".pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }

    @PostMapping("/{id}/revise")
    @PreAuthorize("hasAuthority('BOOKING_REVISE')")
    @Operation(summary = "Reset an APPROVED booking back to BOOKED for editing (only if no AWB and print not taken)")
    public BookingResponse revise(@PathVariable Long id) {
        return bookingService.revise(id);
    }

    @PostMapping("/{id}/request-cancellation")
    @PreAuthorize("hasAuthority('BOOKING_UPDATE')")
    @Operation(summary = "Request cancellation; routes through same approval flow as creation")
    public BookingResponse requestCancellation(@PathVariable Long id,
                                               @RequestParam(required = false) String remarks) {
        return bookingService.requestCancellation(id, remarks);
    }

    @PostMapping("/{id}/approve-cancellation")
    @PreAuthorize("hasAuthority('BOOKING_APPROVE')")
    @Operation(summary = "Approver confirms cancellation (PENDING_CANCELLATION → CANCELLED)")
    public BookingResponse approveCancellation(@PathVariable Long id, Authentication authentication) {
        return bookingService.approveCancellation(id, authentication.getName());
    }

    @PostMapping("/{id}/reject-cancellation")
    @PreAuthorize("hasAuthority('BOOKING_APPROVE')")
    @Operation(summary = "Approver rejects cancellation (PENDING_CANCELLATION → APPROVED)")
    public BookingResponse rejectCancellation(@PathVariable Long id, Authentication authentication) {
        return bookingService.rejectCancellation(id, authentication.getName());
    }

    @GetMapping("/my-company-settings")
    @Operation(summary = "Get company settings for the authenticated user's company (sender auto-fill)")
    public ResponseEntity<CompanySettingsResponse> myCompanySettings(Authentication authentication) {
        return userRepository.findByUsername(authentication.getName())
                .filter(u -> u.getCompany() != null)
                .map(u -> ResponseEntity.ok(adminService.getCompanySettingsByCompanyId(u.getCompany().getId())))
                .orElse(ResponseEntity.noContent().build());
    }
}
