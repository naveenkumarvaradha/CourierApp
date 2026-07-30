package com.courierapp.dto.dc;

import com.courierapp.enums.CourierMode;
import com.courierapp.enums.DcType;
import com.courierapp.enums.ReceiverType;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;

public record DcRequest(
        @NotNull(message = "Unit is required")
        Long unitId,

        @NotNull(message = "DC type is required")
        DcType dcType,

        @NotNull(message = "Receiver type is required")
        ReceiverType receiverType,

        Long receiverPartyId,

        Long receiverUnitId,

        @NotNull(message = "Courier way is required")
        Long courierWayId,

        Long packageTypeId,

        @NotBlank(message = "Item description is required")
        @Size(max = 500)
        String itemDescription,

        @NotNull(message = "Weight is required")
        @DecimalMin(value = "0.001", message = "Weight must be greater than 0")
        BigDecimal weightKg,

        @NotNull(message = "Number of packages is required")
        @Min(value = 1, message = "At least 1 package is required")
        Integer noOfPackages,

        @NotNull(message = "Courier mode is required")
        CourierMode courierMode,

        @Size(max = 30)
        String vehicleNumber,

        @Size(max = 100)
        String driverName,

        @Size(max = 500)
        String remarks
) {
}
