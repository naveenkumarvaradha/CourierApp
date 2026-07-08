package com.courierapp.kafka;

import com.courierapp.kafka.event.PartyEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@ConditionalOnProperty(name = "app.kafka.enabled", havingValue = "true")
public class PartyEventConsumer {

    @KafkaListener(topics = CourierEventProducer.TOPIC_PARTIES, groupId = "courier-app-notifications")
    public void onPartyEvent(PartyEvent event) {
        switch (event.eventType()) {
            case "PARTY_CREATED" ->
                log.info("[EVENT] New party {} ({}) created by {} — awaiting approval",
                        event.partyCode(), event.partyName(), event.createdBy());

            case "PARTY_APPROVED" ->
                log.info("[EVENT] Party {} ({}) APPROVED by {}. Now active.",
                        event.partyCode(), event.partyName(), event.actionBy());

            case "PARTY_REJECTED" ->
                log.info("[EVENT] Party {} ({}) REJECTED by {}. Reason: {}",
                        event.partyCode(), event.partyName(), event.actionBy(), event.remarks());

            default ->
                log.warn("[EVENT] Unknown party event type: {}", event.eventType());
        }
    }
}
