package com.eventhub.api.consumer;

import com.eventhub.api.dto.EventMessage;
import com.eventhub.api.entity.EventStatus;
import com.eventhub.api.service.EventService;
import com.eventhub.api.service.InMemoryEventPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Consumer en modo demo (sin Kafka).
 * Activo cuando app.kafka.enabled=false.
 * Hace polling cada 2 segundos a la cola en memoria para simular el comportamiento de Kafka.
 */
@Component
@ConditionalOnProperty(name = "app.kafka.enabled", havingValue = "false")
@RequiredArgsConstructor
@Slf4j
public class InMemoryEventConsumer {

    private final EventService eventService;
    private final InMemoryEventPublisher publisher;

    @Scheduled(fixedDelay = 2000) // Poll every 2 seconds
    public void consumeFromQueue() {
        EventMessage message = publisher.poll();

        if (message != null) {
            log.info("[DEMO MODE] Processing event from in-memory queue: eventId={}",
                    message.getEventId());

            try {
                // Simula tiempo de procesamiento (1-2 segundos)
                Thread.sleep(1000 + (long) (Math.random() * 1000));

                eventService.updateEventStatus(message.getEventId(), EventStatus.PROCESSED);

                log.info("[DEMO MODE] Event processed successfully: eventId={}",
                        message.getEventId());

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.error("[DEMO MODE] Event processing interrupted: eventId={}",
                        message.getEventId());
                eventService.updateEventStatus(message.getEventId(), EventStatus.FAILED);

            } catch (Exception e) {
                log.error("[DEMO MODE] Error processing event: eventId={}, error={}",
                        message.getEventId(), e.getMessage());
                eventService.updateEventStatus(message.getEventId(), EventStatus.FAILED);
            }
        }
    }
}
