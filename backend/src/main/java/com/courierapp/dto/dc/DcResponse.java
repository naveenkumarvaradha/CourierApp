package com.courierapp.dto.dc;

import com.courierapp.dto.admin.CourierWayResponse;
import com.courierapp.dto.admin.PackageTypeResponse;
import com.courierapp.dto.admin.UnitResponse;
import com.courierapp.dto.master.PartyResponse;
import com.courierapp.enums.CourierMode;
import com.courierapp.enums.DcStatus;
import com.courierapp.enums.DcType;
import com.courierapp.enums.ReceiverType;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public record DcResponse(
        Long id,
        String dcNumber,
        LocalDate dcDate,
        DcType dcType,
        UnitResponse unit,
        ReceiverType receiverType,
        PartyResponse receiverParty,
        UnitResponse receiverUnit,
        CourierWayResponse courierWay,
        PackageTypeResponse packageType,
        String itemDescription,
        BigDecimal weightKg,
        Integer noOfPackages,
        CourierMode courierMode,
        String vehicleNumber,
        String driverName,
        DcStatus status,
        String remarks,
        int currentApprovalLevel,
        String approverUsername,
        Instant approvalTimestamp,
        String approvalRemarks,
        Instant createdAt,
        String createdBy,
        Instant updatedAt,
        String updatedBy,
        List<String> pendingApprovers
) {
}
