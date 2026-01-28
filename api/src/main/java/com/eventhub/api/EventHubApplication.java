package com.eventhub.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * EventHub Cloud Application
 * 
 * A demonstration of event-driven architecture using:
 * - Spring Boot 3.x
 * - Apache Kafka (or in-memory queue for demo mode)
 * - PostgreSQL
 * - RESTful APIs with OpenAPI documentation
 */
@SpringBootApplication
@EnableScheduling
public class EventHubApplication {

    public static void main(String[] args) {
        SpringApplication.run(EventHubApplication.class, args);
    }
}
