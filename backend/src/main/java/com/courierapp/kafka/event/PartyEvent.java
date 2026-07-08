package com.courierapp.kafka.event;

import java.time.Instant;

public record PartyEvent(
        String eventType,        // PARTY_CREATED, PARTY_APPROVED, PARTY_REJECTED
        Long partyId,
        String partyCode,
        String partyName,
        String status,
        String createdBy,
        String actionBy,
        String remarks,
        Instant occurredAt
) {
    public static PartyEvent created(Long id, String code, String name, String createdBy) {
        return new PartyEvent("PARTY_CREATED", id, code, name, "PENDING_APPROVAL", createdBy, createdBy, null, Instant.now());
    }

    public static PartyEvent approved(Long id, String code, String name, String createdBy, String approver) {
        return new PartyEvent("PARTY_APPROVED", id, code, name, "ACTIVE", createdBy, approver, null, Instant.now());
    }

    public static PartyEvent rejected(Long id, String code, String name, String createdBy, String approver, String remarks) {
        return new PartyEvent("PARTY_REJECTED", id, code, name, "REJECTED", createdBy, approver, remarks, Instant.now());
    }
}
