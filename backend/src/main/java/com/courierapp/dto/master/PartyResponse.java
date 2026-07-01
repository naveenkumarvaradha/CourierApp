package com.courierapp.dto.master;

import com.courierapp.enums.PartyStatus;
import com.courierapp.enums.PartyType;

import java.time.Instant;

public record PartyResponse(
        Long id,
        String partyCode,
        String partyName,
        String addressLine1,
        String addressLine2,
        String city,
        String state,
        String pincode,
        String country,
        String phone,
        String email,
        String gstin,
        PartyType partyType,
        boolean active,
        PartyStatus partyStatus,
        Instant createdAt,
        String createdBy,
        Instant updatedAt,
        String updatedBy
) {
}
