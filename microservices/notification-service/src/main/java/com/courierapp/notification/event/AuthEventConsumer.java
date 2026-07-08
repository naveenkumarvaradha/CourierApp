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
public class AuthEventConsumer {

    private final EmailService emailService;

    @KafkaListener(topics = "auth.events", groupId = "${spring.kafka.consumer.group-id:notification-service}",
            containerFactory = "kafkaListenerContainerFactory")
    public void consume(Map<String, Object> event) {
        if (event == null) return;
        String eventType = String.valueOf(event.getOrDefault("eventType", ""));
        log.info("Received auth event: type={}", eventType);

        String toEmail = String.valueOf(event.getOrDefault("email", ""));
        String username = String.valueOf(event.getOrDefault("username", ""));

        if (toEmail.isBlank() || toEmail.equals("null")) {
            log.warn("No email in auth event, skipping notification");
            return;
        }

        switch (eventType) {
            case "PASSWORD_RESET_REQUESTED" -> {
                String token = String.valueOf(event.getOrDefault("resetToken", ""));
                emailService.sendPasswordResetEmail(toEmail, username, token);
            }
            case "USER_CREATED" -> {
                String tempPassword = String.valueOf(event.getOrDefault("tempPassword", ""));
                emailService.sendWelcomeEmail(toEmail, username, tempPassword);
            }
            default -> log.debug("Unhandled auth event type: {}", eventType);
        }
    }
}
