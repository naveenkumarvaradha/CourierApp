package com.courierapp.booking.dto.booking;

import com.courierapp.booking.enums.CourierMode;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;

public record BookingRequest(
        @NotNull(message = "Receiver is required")
        Long receiverId,

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

        @Size(max = 1000)
        String specialInstructions,

        @Size(max = 100)
        String companyPoNo
) {}
