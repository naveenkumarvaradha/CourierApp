package com.courierapp.kafka;

import com.courierapp.kafka.event.BookingEvent;
import com.courierapp.kafka.event.PartyEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class CourierEventProducer {

    public static final String TOPIC_BOOKINGS = "courier.bookings";
    public static final String TOPIC_PARTIES  = "courier.parties";

    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Value("${app.kafka.enabled:false}")
    private boolean kafkaEnabled;

    public CourierEventProducer(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void publishBookingEvent(BookingEvent event) {
        if (!kafkaEnabled) {
            log.debug("[KAFKA DISABLED] BookingEvent {} for {}", event.eventType(), event.bookingNumber());
            return;
        }
        kafkaTemplate.send(TOPIC_BOOKINGS, event.bookingNumber(), event)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("Failed to publish BookingEvent {} for booking {}: {}",
                                event.eventType(), event.bookingNumber(), ex.getMessage());
                    } else {
                        log.debug("Published BookingEvent {} for booking {} to partition {}",
                                event.eventType(), event.bookingNumber(),
                                result.getRecordMetadata().partition());
                    }
                });
    }

    public void publishPartyEvent(PartyEvent event) {
        if (!kafkaEnabled) {
            log.debug("[KAFKA DISABLED] PartyEvent {} for {}", event.eventType(), event.partyCode());
            return;
        }
        kafkaTemplate.send(TOPIC_PARTIES, event.partyCode(), event)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("Failed to publish PartyEvent {} for party {}: {}",
                                event.eventType(), event.partyCode(), ex.getMessage());
                    } else {
                        log.debug("Published PartyEvent {} for party {} to partition {}",
                                event.eventType(), event.partyCode(),
                                result.getRecordMetadata().partition());
                    }
                });
    }
}
