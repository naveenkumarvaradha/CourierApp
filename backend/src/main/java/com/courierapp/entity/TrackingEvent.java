package com.courierapp.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.time.OffsetDateTime;

@Entity
@Table(name = "tracking_events")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TrackingEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "booking_id", nullable = false)
    private Booking booking;

    /** Courier way name the event came from, e.g. "DHL", "MARUTI". */
    @Column(name = "provider", nullable = false, length = 30)
    private String provider;

    @Column(name = "status", nullable = false, length = 100)
    private String status;

    @Column(name = "description", length = 500)
    private String description;

    @Column(name = "location", length = 255)
    private String location;

    @Column(name = "event_time")
    private OffsetDateTime eventTime;

    /** Raw JSON payload from the carrier, kept for debugging/audit. */
    @Column(name = "raw_payload", columnDefinition = "text")
    private String rawPayload;

    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @PrePersist
    void onCreate() {
        if (createdAt == null) createdAt = Instant.now();
    }
}
