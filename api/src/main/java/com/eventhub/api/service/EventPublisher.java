package com.eventhub.api.service;

import com.eventhub.api.dto.EventMessage;

/**
 * Interface for event publishing.
 * Allows for different implementations (Kafka, In-Memory, etc.)
 */
public interface EventPublisher {

    /**
     * Publish an event message to the messaging system.
     * 
     * @param message The event message to publish
     */
    void publish(EventMessage message);

    /**
     * Check if the publisher is using Kafka.
     * 
     * @return true if Kafka is enabled, false otherwise
     */
    boolean isKafkaEnabled();
}
