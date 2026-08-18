package com.courierapp.service.tracking;

import com.courierapp.dto.tracking.TrackingEventResponse;
import com.courierapp.entity.Booking;
import com.courierapp.entity.TrackingEvent;
import com.courierapp.exception.BusinessException;
import com.courierapp.exception.ResourceNotFoundException;
import com.courierapp.repository.BookingRepository;
import com.courierapp.repository.TrackingEventRepository;
import com.courierapp.security.CurrentUserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
public class TrackingService {

    private final BookingRepository bookingRepository;
    private final TrackingEventRepository trackingEventRepository;
    private final Map<String, TrackingProvider> providersByCourierWay;
    private final ObjectMapper objectMapper;
    private final CurrentUserService currentUserService;

    public TrackingService(BookingRepository bookingRepository,
                            TrackingEventRepository trackingEventRepository,
                            List<TrackingProvider> providers,
                            ObjectMapper objectMapper,
                            CurrentUserService currentUserService) {
        this.bookingRepository = bookingRepository;
        this.trackingEventRepository = trackingEventRepository;
        this.objectMapper = objectMapper;
        this.currentUserService = currentUserService;
        this.providersByCourierWay = providers.stream()
                .collect(Collectors.toMap(
                        p -> p.courierWayName().toUpperCase(),
                        p -> p));
    }

    /**
     * Fetches live tracking from the carrier for the booking's AWB, persists
     * any new events, updates the booking's cached status, and returns the
     * full event history (newest first).
     */
    @Transactional
    public List<TrackingEventResponse> trackAndSync(Long bookingId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking", bookingId));

        Long callerCompanyId = currentUserService.requireCompanyId();
        Long ownerCompanyId = booking.getSender().getCompany() != null
                ? booking.getSender().getCompany().getId() : null;
        if (ownerCompanyId == null || !ownerCompanyId.equals(callerCompanyId)) {
            // 404, not 403 — mirrors BookingServiceImpl.findBooking's IDOR protection.
            throw new ResourceNotFoundException("Booking", bookingId);
        }

        if (booking.getAwbNumber() == null || booking.getAwbNumber().isBlank()) {
            throw new BusinessException("AWB number must be set before tracking is available.");
        }
        if (booking.getCourierWay() == null) {
            throw new BusinessException("Booking has no courier way set.");
        }

        String courierWayName = booking.getCourierWay().getName();
        TrackingProvider provider = providersByCourierWay.get(courierWayName.toUpperCase());
        if (provider == null) {
            throw new BusinessException("No tracking integration configured for courier way '" + courierWayName + "'.");
        }

        List<TrackingEventResponse> liveEvents = provider.track(booking.getAwbNumber());

        // Dedupe against what's already stored (matches the DB unique index on
        // booking_id/status/event_time) before inserting — catching the constraint
        // violation instead would abort the whole transaction under Postgres.
        var existingKeys = trackingEventRepository.findByBookingIdOrderByEventTimeDesc(bookingId).stream()
                .map(ev -> dedupeKey(ev.getStatus(), ev.getEventTime()))
                .collect(Collectors.toSet());

        for (TrackingEventResponse e : liveEvents) {
            if (!existingKeys.add(dedupeKey(e.status(), e.eventTime()))) continue;
            TrackingEvent entity = TrackingEvent.builder()
                    .booking(booking)
                    .provider(e.provider())
                    .status(e.status())
                    .description(e.description())
                    .location(e.location())
                    .eventTime(e.eventTime())
                    .rawPayload(toJson(e))
                    .build();
            trackingEventRepository.save(entity);
        }

        if (!liveEvents.isEmpty()) {
            TrackingEventResponse latest = liveEvents.stream()
                    .max(Comparator.comparing(e -> e.eventTime() == null ? OffsetDateTime.MIN : e.eventTime()))
                    .orElse(liveEvents.get(0));
            booking.setTrackingStatus(latest.status());
        }
        booking.setLastTrackedAt(OffsetDateTime.now());
        bookingRepository.save(booking);

        return trackingEventRepository.findByBookingIdOrderByEventTimeDesc(bookingId).stream()
                .map(this::toResponse)
                .toList();
    }

    /** Mirrors the DB unique index (booking is implicit — always scoped per-booking here). */
    private static String dedupeKey(String status, OffsetDateTime eventTime) {
        return status + "|" + (eventTime == null ? "" : eventTime);
    }

    private TrackingEventResponse toResponse(TrackingEvent e) {
        return new TrackingEventResponse(e.getProvider(), e.getStatus(), e.getDescription(), e.getLocation(), e.getEventTime());
    }

    private String toJson(TrackingEventResponse e) {
        try {
            return objectMapper.writeValueAsString(e);
        } catch (Exception ex) {
            return null;
        }
    }
}
