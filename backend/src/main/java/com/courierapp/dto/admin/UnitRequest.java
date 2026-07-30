package com.courierapp.dto.admin;

import jakarta.validation.constraints.*;

public record UnitRequest(
        @NotBlank(message = "Unit name is required")
        @Size(max = 150)
        String unitName,

        @NotBlank(message = "Address line 1 is required")
        @Size(max = 200)
        String addressLine1,

        @Size(max = 200)
        String addressLine2,

        @NotBlank(message = "City is required")
        @Size(max = 100)
        String city,

        @NotBlank(message = "State is required")
        @Size(max = 100)
        String state,

        @NotBlank(message = "Pincode is required")
        @Size(max = 20)
        String pincode,

        @NotBlank(message = "Country is required")
        @Size(max = 100)
        String country,

        @Size(max = 30)
        String phone,

        @Email(message = "Email must be valid")
        @Size(max = 150)
        String email,

        @Size(max = 20)
        String gstin,

        boolean defaultUnit,

        boolean active
) {
}
