package com.courierapp.controller;

import com.courierapp.dto.PageResponse;
import com.courierapp.dto.booking.ApprovalDecisionRequest;
import com.courierapp.dto.booking.AwbUpdateRequest;
import com.courierapp.dto.booking.BookingRequest;
import com.courierapp.dto.booking.BookingResponse;
import com.courierapp.dto.booking.StatusUpdateRequest;
import com.courierapp.enums.BookingStatus;
import com.courierapp.enums.CourierMode;
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

    public BookingController(BookingService bookingService) {
        this.bookingService = bookingService;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('BOOKING_VIEW')")
    @Operation(summary = "Search/list bookings with filters and pagination")
    public PageResponse<BookingResponse> search(
            @RequestParam(required = false) String bookingNumber,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            @RequestParam(required = false) BookingStatus status,
            @RequestParam(required = false) Long senderId,
            @RequestParam(required = false) Long receiverId,
            @RequestParam(required = false) CourierMode mode,
            @PageableDefault(size = 20, sort = "id") Pageable pageable) {
        return bookingService.search(bookingNumber, fromDate, toDate, status, senderId, receiverId, mode, pageable);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('BOOKING_VIEW')")
    public BookingResponse get(@PathVariable Long id) {
        return bookingService.get(id);
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
    @Operation(summary = "Submit a BOOKED booking for approval")
    public BookingResponse submit(@PathVariable Long id) {
        return bookingService.submitForApproval(id);
    }

    @PostMapping("/{id}/approve")
    @PreAuthorize("hasAuthority('BOOKING_APPROVE')")
    @Operation(summary = "Approve a pending booking (must be a designated approver)")
    public BookingResponse approve(@PathVariable Long id,
                                   @Valid @RequestBody(required = false) ApprovalDecisionRequest request,
                                   Authentication authentication) {
        return bookingService.approve(id, request, authentication.getName());
    }

    @PostMapping("/{id}/reject")
    @PreAuthorize("hasAuthority('BOOKING_APPROVE')")
    @Operation(summary = "Reject a pending booking (must be a designated approver)")
    public BookingResponse reject(@PathVariable Long id,
                                  @Valid @RequestBody(required = false) ApprovalDecisionRequest request,
                                  Authentication authentication) {
        return bookingService.reject(id, request, authentication.getName());
    }

    @PostMapping("/{id}/status")
    @PreAuthorize("hasAuthority('BOOKING_UPDATE')")
    @Operation(summary = "Advance booking lifecycle status (IN_TRANSIT, DELIVERED, CANCELLED)")
    public BookingResponse changeStatus(@PathVariable Long id,
                                        @Valid @RequestBody StatusUpdateRequest request) {
        return bookingService.changeStatus(id, request);
    }

    @PutMapping("/{id}/awb")
    @PreAuthorize("hasAuthority('BOOKING_UPDATE')")
    @Operation(summary = "Set or update the AWB number on an APPROVED booking (required before sticker print)")
    public BookingResponse updateAwb(@PathVariable Long id,
                                     @Valid @RequestBody AwbUpdateRequest request) {
        return bookingService.updateAwb(id, request);
    }

    @GetMapping(value = "/{id}/sticker", produces = MediaType.APPLICATION_PDF_VALUE)
    @PreAuthorize("hasAuthority('BOOKING_VIEW')")
    @Operation(summary = "Generate a printable 4x6 shipping label PDF with barcode")
    public ResponseEntity<byte[]> sticker(@PathVariable Long id) {
        byte[] pdf = bookingService.generateStickerPdf(id);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=sticker-" + id + ".pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }
}
