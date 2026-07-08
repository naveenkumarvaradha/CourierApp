package com.courierapp.config;

import com.courierapp.kafka.CourierEventProducer;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.listener.CommonLoggingErrorHandler;

@Configuration
public class KafkaConfig {

    @Bean
    @ConditionalOnProperty(name = "app.kafka.enabled", havingValue = "true")
    public NewTopic bookingsTopic() {
        return TopicBuilder.name(CourierEventProducer.TOPIC_BOOKINGS)
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    @ConditionalOnProperty(name = "app.kafka.enabled", havingValue = "true")
    public NewTopic partiesTopic() {
        return TopicBuilder.name(CourierEventProducer.TOPIC_PARTIES)
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public KafkaTemplate<String, Object> kafkaTemplate(ProducerFactory<String, Object> producerFactory) {
        return new KafkaTemplate<>(producerFactory);
    }

    @Bean
    public CommonLoggingErrorHandler kafkaErrorHandler() {
        return new CommonLoggingErrorHandler();
    }
}
