package com.courierapp.kafka;

import com.courierapp.kafka.event.BookingEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@ConditionalOnProperty(name = "app.kafka.enabled", havingValue = "true")
public class BookingEventConsumer {

    @KafkaListener(topics = CourierEventProducer.TOPIC_BOOKINGS, groupId = "courier-app-notifications")
    public void onBookingEvent(BookingEvent event) {
        switch (event.eventType()) {
            case "BOOKING_CREATED" ->
                log.info("[EVENT] New booking {} created by {} (company: {})",
                        event.bookingNumber(), event.createdBy(), event.companyCode());

            case "BOOKING_SUBMITTED" ->
                log.info("[EVENT] Booking {} submitted for approval by {}",
                        event.bookingNumber(), event.createdBy());

            case "BOOKING_APPROVED" ->
                log.info("[EVENT] Booking {} APPROVED by {}. Remarks: {}",
                        event.bookingNumber(), event.actionBy(), event.remarks());

            case "BOOKING_REJECTED" ->
                log.info("[EVENT] Booking {} REJECTED by {}. Reason: {}",
                        event.bookingNumber(), event.actionBy(), event.remarks());

            case "BOOKING_STATUS_CHANGED" ->
                log.info("[EVENT] Booking {} status changed to {} by {}",
                        event.bookingNumber(), event.status(), event.actionBy());

            default ->
                log.warn("[EVENT] Unknown booking event type: {}", event.eventType());
        }
    }
}
