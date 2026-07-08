package com.courierapp.booking.dto.booking;

import com.courierapp.booking.enums.BookingStatus;
import com.courierapp.booking.enums.CourierMode;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public record BookingResponse(
        Long id,
        String bookingNumber,
        LocalDate bookingDate,
        Long senderId,
        String senderName,
        Long receiverId,
        String receiverName,
        Long courierWayId,
        String courierWayName,
        Long packageTypeId,
        String packageTypeName,
        String itemDescription,
        BigDecimal weightKg,
        Integer noOfPackages,
        CourierMode courierMode,
        String specialInstructions,
        BookingStatus status,
        String awbNumber,
        String approverUsername,
        Instant approvalTimestamp,
        String approvalRemarks,
        String companyPoNo,
        boolean printTaken,
        String cancellationRemarks,
        int currentApprovalLevel,
        Instant createdAt,
        String createdBy,
        Instant updatedAt,
        String updatedBy,
        List<String> pendingApprovers
) {}
