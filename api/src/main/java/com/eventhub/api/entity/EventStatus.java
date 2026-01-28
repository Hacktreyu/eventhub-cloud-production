package com.eventhub.api.entity;

/**
 * Enum representing the processing status of an event.
 */
public enum EventStatus {
    PENDING,    // Event created, waiting to be processed
    PROCESSING, // Event is currently being processed
    PROCESSED,  // Event successfully processed
    FAILED      // Event processing failed
}
