package com.courierapp.service;

import com.courierapp.dto.PageResponse;
import com.courierapp.dto.booking.ApprovalDecisionRequest;
import com.courierapp.dto.booking.AwbUpdateRequest;
import com.courierapp.dto.booking.BookingRequest;
import com.courierapp.dto.booking.BookingResponse;
import com.courierapp.dto.booking.StatusUpdateRequest;
import com.courierapp.enums.BookingStatus;
import com.courierapp.enums.CourierMode;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;

public interface BookingService {

    PageResponse<BookingResponse> search(String bookingNumber, LocalDate fromDate, LocalDate toDate,
                                         BookingStatus status, Long senderId, Long receiverId,
                                         CourierMode mode, Pageable pageable);

    BookingResponse get(Long id);

    BookingResponse create(BookingRequest request);

    BookingResponse update(Long id, BookingRequest request);

    void delete(Long id);

    BookingResponse submitForApproval(Long id);

    BookingResponse approve(Long id, ApprovalDecisionRequest request, String approverUsername);

    BookingResponse reject(Long id, ApprovalDecisionRequest request, String approverUsername);

    BookingResponse changeStatus(Long id, StatusUpdateRequest request);

    BookingResponse updateAwb(Long id, AwbUpdateRequest request);

    byte[] generateStickerPdf(Long id);

    /** Reset an APPROVED booking back to BOOKED for re-editing (only if no AWB and print not taken). */
    BookingResponse revise(Long id);

    /** Request cancellation for an APPROVED booking; routes through approval. */
    BookingResponse requestCancellation(Long id, String remarks);

    /** Approver confirms cancellation (PENDING_CANCELLATION → CANCELLED). */
    BookingResponse approveCancellation(Long id, String approverUsername);

    /** Approver rejects cancellation (PENDING_CANCELLATION → APPROVED). */
    BookingResponse rejectCancellation(Long id, String approverUsername);
}
