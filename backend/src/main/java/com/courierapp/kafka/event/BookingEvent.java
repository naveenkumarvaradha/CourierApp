package com.courierapp.kafka.event;

import java.time.Instant;

public record BookingEvent(
        String eventType,        // BOOKING_CREATED, BOOKING_SUBMITTED, BOOKING_APPROVED, BOOKING_REJECTED, BOOKING_STATUS_CHANGED
        Long bookingId,
        String bookingNumber,
        String status,
        String createdBy,
        String actionBy,
        String companyCode,
        String remarks,
        Instant occurredAt
) {
    public static BookingEvent created(Long id, String number, String createdBy, String companyCode) {
        return new BookingEvent("BOOKING_CREATED", id, number, "BOOKED", createdBy, createdBy, companyCode, null, Instant.now());
    }

    public static BookingEvent submitted(Long id, String number, String createdBy) {
        return new BookingEvent("BOOKING_SUBMITTED", id, number, "PENDING_APPROVAL", createdBy, createdBy, null, null, Instant.now());
    }

    public static BookingEvent approved(Long id, String number, String createdBy, String approver, String remarks) {
        return new BookingEvent("BOOKING_APPROVED", id, number, "APPROVED", createdBy, approver, null, remarks, Instant.now());
    }

    public static BookingEvent rejected(Long id, String number, String createdBy, String approver, String remarks) {
        return new BookingEvent("BOOKING_REJECTED", id, number, "REJECTED", createdBy, approver, null, remarks, Instant.now());
    }

    public static BookingEvent statusChanged(Long id, String number, String newStatus, String actionBy) {
        return new BookingEvent("BOOKING_STATUS_CHANGED", id, number, newStatus, null, actionBy, null, null, Instant.now());
    }
}
