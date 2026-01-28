package com.eventhub.api.consumer;

import com.eventhub.api.dto.EventMessage;
import com.eventhub.api.entity.EventStatus;
import com.eventhub.api.service.EventService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Consumer de Kafka para procesar eventos.
 * Activo cuando app.kafka.enabled=true.
 */
@Component
@ConditionalOnProperty(name = "app.kafka.enabled", havingValue = "true")
@RequiredArgsConstructor
@Slf4j
public class KafkaEventConsumer {

    private final EventService eventService;

    @KafkaListener(topics = "${app.kafka.topic.events}", groupId = "${spring.kafka.consumer.group-id}")
    public void consume(EventMessage message) {
        log.info("Received event from Kafka: eventId={}, title='{}'",
                message.getEventId(), message.getTitle());

        try {
            // Simula tiempo de procesamiento (1-2 segundos)
            Thread.sleep(1000 + (long) (Math.random() * 1000));

            eventService.updateEventStatus(message.getEventId(), EventStatus.PROCESSED);

            log.info("Event processed successfully: eventId={}", message.getEventId());

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Event processing interrupted: eventId={}", message.getEventId());
            eventService.updateEventStatus(message.getEventId(), EventStatus.FAILED);

        } catch (Exception e) {
            log.error("Error processing event: eventId={}, error={}",
                    message.getEventId(), e.getMessage());
            eventService.updateEventStatus(message.getEventId(), EventStatus.FAILED);
        }
    }
}
