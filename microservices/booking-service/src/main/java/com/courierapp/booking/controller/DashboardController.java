package com.courierapp.booking.controller;

import com.courierapp.booking.dto.booking.BookingResponse;
import com.courierapp.booking.dto.dashboard.DashboardResponse;
import com.courierapp.booking.entity.Booking;
import com.courierapp.booking.enums.BookingStatus;
import com.courierapp.booking.repository.BookingRepository;
import com.courierapp.booking.service.ApprovalAuthorizationService;
import com.courierapp.booking.service.impl.BookingServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final BookingRepository bookingRepository;
    private final ApprovalAuthorizationService approvalAuthorizationService;
    private final BookingServiceImpl bookingService;

    @GetMapping
    public DashboardResponse get(Authentication auth) {
        String username = auth.getName();
        boolean isAdmin = auth.getAuthorities().stream()
                .anyMatch(a -> "ADMIN_VIEW".equals(a.getAuthority()));
        boolean canApproveBookings = isAdmin || auth.getAuthorities().stream()
                .anyMatch(a -> "BOOKING_APPROVE".equals(a.getAuthority()));

        // Bookings pending my approval
        List<BookingResponse> bookingsPendingMyApproval = List.of();
        if (canApproveBookings) {
            Specification<Booking> spec = (root, q, cb) -> cb.and(
                    cb.equal(root.get("status"), BookingStatus.PENDING_APPROVAL),
                    cb.notEqual(root.get("createdBy"), username)
            );
            bookingsPendingMyApproval = bookingRepository.findAll(spec).stream()
                    .filter(b -> approvalAuthorizationService.isDesignatedApproverAtLevel(
                            username, b.getCreatedBy(), "BOOKING", b.getCurrentApprovalLevel()))
                    .map(bookingService::toResponsePublic)
                    .toList();
        }

        // Bookings I sent that are pending approval
        Specification<Booking> mySentSpec = (root, q, cb) -> cb.and(
                cb.equal(root.get("status"), BookingStatus.PENDING_APPROVAL),
                cb.equal(root.get("createdBy"), username)
        );
        List<BookingResponse> myBookingsPendingSent = bookingRepository.findAll(mySentSpec)
                .stream().map(b -> {
                    BookingResponse base = bookingService.toResponsePublic(b);
                    List<String> approvers = approvalAuthorizationService
                            .resolveApproversAtLevel(b.getCreatedBy(), "BOOKING", b.getCurrentApprovalLevel());
                    return new BookingResponse(base.id(), base.bookingNumber(), base.bookingDate(),
                            base.senderId(), base.senderName(), base.receiverId(), base.receiverName(),
                            base.courierWayId(), base.courierWayName(), base.packageTypeId(), base.packageTypeName(),
                            base.itemDescription(), base.weightKg(), base.noOfPackages(),
                            base.courierMode(), base.specialInstructions(), base.status(),
                            base.awbNumber(), base.approverUsername(), base.approvalTimestamp(),
                            base.approvalRemarks(), base.companyPoNo(), base.printTaken(),
                            base.cancellationRemarks(), base.currentApprovalLevel(),
                            base.createdAt(), base.createdBy(), base.updatedAt(), base.updatedBy(),
                            approvers);
                }).toList();

        // Approved bookings pending print
        boolean canPrint = auth.getAuthorities().stream()
                .anyMatch(a -> "BOOKING_PRINT".equals(a.getAuthority()));
        List<BookingResponse> pendingToPrint = List.of();
        if (canPrint) {
            Specification<Booking> printSpec = (root, q, cb) -> cb.and(
                    cb.equal(root.get("status"), BookingStatus.APPROVED),
                    cb.equal(root.get("printTaken"), false)
            );
            pendingToPrint = bookingRepository.findAll(printSpec)
                    .stream().map(bookingService::toResponsePublic).toList();
        }

        // All pending approval bookings (for BOOKING_VIEW users)
        boolean canViewBookings = isAdmin || auth.getAuthorities().stream()
                .anyMatch(a -> "BOOKING_VIEW".equals(a.getAuthority()));
        List<BookingResponse> allPendingApprovalBookings = List.of();
        if (canViewBookings) {
            Specification<Booking> allPendingSpec = (root, q, cb) ->
                    cb.equal(root.get("status"), BookingStatus.PENDING_APPROVAL);
            allPendingApprovalBookings = bookingRepository.findAll(allPendingSpec)
                    .stream().map(b -> {
                        BookingResponse base = bookingService.toResponsePublic(b);
                        List<String> approvers = approvalAuthorizationService
                                .resolveApproversAtLevel(b.getCreatedBy(), "BOOKING", b.getCurrentApprovalLevel());
                        return new BookingResponse(base.id(), base.bookingNumber(), base.bookingDate(),
                                base.senderId(), base.senderName(), base.receiverId(), base.receiverName(),
                                base.courierWayId(), base.courierWayName(), base.packageTypeId(), base.packageTypeName(),
                                base.itemDescription(), base.weightKg(), base.noOfPackages(),
                                base.courierMode(), base.specialInstructions(), base.status(),
                                base.awbNumber(), base.approverUsername(), base.approvalTimestamp(),
                                base.approvalRemarks(), base.companyPoNo(), base.printTaken(),
                                base.cancellationRemarks(), base.currentApprovalLevel(),
                                base.createdAt(), base.createdBy(), base.updatedAt(), base.updatedBy(),
                                approvers);
                    }).toList();
        }

        return new DashboardResponse(bookingsPendingMyApproval, myBookingsPendingSent,
                pendingToPrint, allPendingApprovalBookings);
    }
}
