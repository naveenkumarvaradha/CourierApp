package com.courierapp.notification.event;

import com.courierapp.notification.service.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.kafka.enabled", havingValue = "true")
public class BookingEventConsumer {

    private final EmailService emailService;

    @KafkaListener(topics = "booking.events", groupId = "${spring.kafka.consumer.group-id:notification-service}",
            containerFactory = "kafkaListenerContainerFactory")
    public void consume(Map<String, Object> event) {
        if (event == null) return;
        String eventType = String.valueOf(event.getOrDefault("eventType", ""));
        log.info("Received booking event: type={}", eventType);

        String toEmail = String.valueOf(event.getOrDefault("creatorEmail", ""));
        String creatorName = String.valueOf(event.getOrDefault("creatorUsername", ""));
        String bookingNumber = String.valueOf(event.getOrDefault("bookingNumber", ""));
        String status = String.valueOf(event.getOrDefault("status", ""));
        String remarks = String.valueOf(event.getOrDefault("remarks", ""));

        if (toEmail.isBlank() || toEmail.equals("null")) {
            log.warn("No creator email in booking event, skipping notification");
            return;
        }

        switch (eventType) {
            case "BOOKING_SUBMITTED" -> emailService.sendBookingStatusEmail(toEmail, creatorName, bookingNumber,
                    "Submitted for Approval", remarks);
            case "BOOKING_APPROVED" -> emailService.sendBookingStatusEmail(toEmail, creatorName, bookingNumber,
                    "Approved", remarks);
            case "BOOKING_REJECTED" -> emailService.sendBookingStatusEmail(toEmail, creatorName, bookingNumber,
                    "Rejected", remarks);
            case "BOOKING_CANCELLATION_REQUESTED" -> emailService.sendBookingStatusEmail(toEmail, creatorName,
                    bookingNumber, "Cancellation Requested", remarks);
            case "BOOKING_CANCELLED" -> emailService.sendBookingStatusEmail(toEmail, creatorName, bookingNumber,
                    "Cancelled", remarks);
            default -> log.debug("Unhandled booking event type: {}", eventType);
        }
    }
}
