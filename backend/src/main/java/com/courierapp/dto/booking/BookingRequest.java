package com.courierapp.dto.booking;

import com.courierapp.enums.CourierMode;
import com.courierapp.enums.PaymentMode;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDate;

public record BookingRequest(
        @NotNull(message = "Booking date is required")
        LocalDate bookingDate,

        @NotNull(message = "Sender is required")
        Long senderId,

        @NotNull(message = "Receiver is required")
        Long receiverId,

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

        @DecimalMin(value = "0.00", message = "Declared value cannot be negative")
        BigDecimal declaredValue,

        @NotNull(message = "Freight charges are required")
        @DecimalMin(value = "0.00", message = "Freight charges cannot be negative")
        BigDecimal freightCharges,

        @NotNull(message = "Total charges are required")
        @DecimalMin(value = "0.00", message = "Total charges cannot be negative")
        BigDecimal totalCharges,

        @NotNull(message = "Payment mode is required")
        PaymentMode paymentMode,

        @Size(max = 1000)
        String specialInstructions
) {
}
