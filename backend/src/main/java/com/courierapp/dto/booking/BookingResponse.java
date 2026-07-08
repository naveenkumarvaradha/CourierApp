package com.courierapp.dto.booking;

import com.courierapp.dto.admin.CourierWayResponse;
import com.courierapp.dto.admin.PackageTypeResponse;
import com.courierapp.dto.master.PartyResponse;
import com.courierapp.enums.BookingStatus;
import com.courierapp.enums.CourierMode;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public record BookingResponse(
        Long id,
        String bookingNumber,
        LocalDate bookingDate,
        PartyResponse sender,
        PartyResponse receiver,
        CourierWayResponse courierWay,
        PackageTypeResponse packageType,
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
