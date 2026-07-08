package com.courierapp.dto.master;

import com.courierapp.enums.PartyType;
import jakarta.validation.constraints.*;

public record PartyRequest(
        @NotBlank(message = "Party name is required")
        @Size(max = 150)
        String partyName,

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

        @NotNull(message = "Party type is required")
        PartyType partyType,

        boolean active,

        @Size(max = 255)
        String companyName
) {
}
