package com.eventhub.api.service;

import com.eventhub.api.dto.EventMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * In-Memory implementation of EventPublisher.
 * Active when app.kafka.enabled=false (demo mode for free tier deployments)
 */
@Service
@ConditionalOnProperty(name = "app.kafka.enabled", havingValue = "false")
@Slf4j
public class InMemoryEventPublisher implements EventPublisher {

    private final BlockingQueue<EventMessage> queue = new LinkedBlockingQueue<>();

    @Override
    public void publish(EventMessage message) {
        log.info("[DEMO MODE] Publishing event to in-memory queue: eventId={}", message.getEventId());
        queue.offer(message);
        log.debug("[DEMO MODE] Queue size: {}", queue.size());
    }

    @Override
    public boolean isKafkaEnabled() {
        return false;
    }

    /**
     * Poll a message from the queue (used by InMemoryEventConsumer)
     */
    public EventMessage poll() {
        return queue.poll();
    }

    /**
     * Get current queue size
     */
    public int getQueueSize() {
        return queue.size();
    }
}
