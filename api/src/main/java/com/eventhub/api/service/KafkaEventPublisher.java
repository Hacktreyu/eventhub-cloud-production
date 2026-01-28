package com.eventhub.api.service;

import com.eventhub.api.dto.EventMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

/**
 * Kafka implementation of EventPublisher.
 * Active when app.kafka.enabled=true
 */
@Service
@ConditionalOnProperty(name = "app.kafka.enabled", havingValue = "true")
@RequiredArgsConstructor
@Slf4j
public class KafkaEventPublisher implements EventPublisher {

    private final KafkaTemplate<String, EventMessage> kafkaTemplate;

    @Value("${app.kafka.topic.events}")
    private String eventsTopic;

    @Override
    public void publish(EventMessage message) {
        log.info("Publishing event to Kafka topic [{}]: eventId={}", eventsTopic, message.getEventId());

        CompletableFuture<SendResult<String, EventMessage>> future = kafkaTemplate.send(eventsTopic,
                String.valueOf(message.getEventId()), message);

        future.whenComplete((result, ex) -> {
            if (ex == null) {
                log.info("Event published successfully: eventId={}, offset={}",
                        message.getEventId(),
                        result.getRecordMetadata().offset());
            } else {
                log.error("Failed to publish event: eventId={}, error={}",
                        message.getEventId(),
                        ex.getMessage());
            }
        });
    }

    @Override
    public boolean isKafkaEnabled() {
        return true;
    }
}
