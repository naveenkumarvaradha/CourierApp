package com.courierapp.booking.service;

import com.courierapp.booking.dto.PageResponse;
import com.courierapp.booking.dto.booking.ApprovalDecisionRequest;
import com.courierapp.booking.dto.booking.AwbUpdateRequest;
import com.courierapp.booking.dto.booking.BookingRequest;
import com.courierapp.booking.dto.booking.BookingResponse;
import com.courierapp.booking.dto.booking.StatusUpdateRequest;
import com.courierapp.booking.enums.BookingStatus;
import com.courierapp.booking.enums.CourierMode;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;

public interface BookingService {

    PageResponse<BookingResponse> search(String bookingNumber, LocalDate fromDate, LocalDate toDate,
                                         BookingStatus status, Long senderId, Long receiverId,
                                         CourierMode mode, String receiverName, String receiverCompanyName,
                                         Pageable pageable);

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

    BookingResponse revise(Long id);

    BookingResponse requestCancellation(Long id, String remarks);

    BookingResponse approveCancellation(Long id, String approverUsername);

    BookingResponse rejectCancellation(Long id, String approverUsername);

    boolean isCreatorOf(Long id, String username);
}
