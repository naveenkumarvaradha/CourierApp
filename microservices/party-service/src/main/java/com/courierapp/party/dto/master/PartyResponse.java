package com.courierapp.party.dto.master;

import com.courierapp.party.enums.PartyStatus;
import com.courierapp.party.enums.PartyType;

import java.time.Instant;
import java.util.List;

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
        String companyName,
        int currentApprovalLevel,
        Instant createdAt,
        String createdBy,
        Instant updatedAt,
        String updatedBy,
        List<String> pendingApprovers
) {}
