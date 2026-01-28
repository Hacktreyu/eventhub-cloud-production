package com.eventhub.api;

import com.eventhub.api.dto.CreateEventRequest;
import com.eventhub.api.dto.EventResponse;
import com.eventhub.api.entity.EventStatus;
import com.eventhub.api.repository.EventRepository;
import com.eventhub.api.service.EventService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests using Testcontainers for PostgreSQL.
 */
@SpringBootTest
@Testcontainers
@ActiveProfiles("demo") // Use demo profile (in-memory queue) for integration tests
class EventIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine")
            .withDatabaseName("eventhub_test")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    private EventService eventService;

    @Autowired
    private EventRepository eventRepository;

    @BeforeEach
    void setUp() {
        eventRepository.deleteAll();
    }

    @Test
    @DisplayName("Should create event and save to database")
    void shouldCreateEvent() {
        // Given
        CreateEventRequest request = CreateEventRequest.builder()
                .title("Test Event")
                .description("Test Description")
                .source("integration-test")
                .type("TEST")
                .build();

        // When
        EventResponse response = eventService.createEvent(request);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getId()).isNotNull();
        assertThat(response.getTitle()).isEqualTo("Test Event");
        assertThat(response.getStatus()).isEqualTo(EventStatus.PENDING);
    }

    @Test
    @DisplayName("Should retrieve all events")
    void shouldGetAllEvents() {
        // Given
        createTestEvent("Event 1");
        createTestEvent("Event 2");
        createTestEvent("Event 3");

        // When
        List<EventResponse> events = eventService.getAllEvents();

        // Then
        assertThat(events).hasSize(3);
    }

    @Test
    @DisplayName("Should update event status")
    void shouldUpdateEventStatus() {
        // Given
        EventResponse created = createTestEvent("Status Test Event");

        // When
        EventResponse updated = eventService.updateEventStatus(
                created.getId(),
                EventStatus.PROCESSED);

        // Then
        assertThat(updated.getStatus()).isEqualTo(EventStatus.PROCESSED);
        assertThat(updated.getProcessedAt()).isNotNull();
    }

    @Test
    @DisplayName("Should get events by status")
    void shouldGetEventsByStatus() {
        // Given
        createTestEvent("Pending 1");
        createTestEvent("Pending 2");
        EventResponse toProcess = createTestEvent("To Process");
        eventService.updateEventStatus(toProcess.getId(), EventStatus.PROCESSED);

        // When
        List<EventResponse> pendingEvents = eventService.getEventsByStatus(EventStatus.PENDING);
        List<EventResponse> processedEvents = eventService.getEventsByStatus(EventStatus.PROCESSED);

        // Then
        assertThat(pendingEvents).hasSize(2);
        assertThat(processedEvents).hasSize(1);
    }

    @Test
    @DisplayName("Should return correct statistics")
    void shouldGetStatistics() {
        // Given
        createTestEvent("Event 1");
        createTestEvent("Event 2");
        EventResponse processed = createTestEvent("Event 3");
        eventService.updateEventStatus(processed.getId(), EventStatus.PROCESSED);

        // When
        EventService.EventStats stats = eventService.getStats();

        // Then
        assertThat(stats.total()).isEqualTo(3);
        assertThat(stats.pending()).isEqualTo(2);
        assertThat(stats.processed()).isEqualTo(1);
    }

    private EventResponse createTestEvent(String title) {
        CreateEventRequest request = CreateEventRequest.builder()
                .title(title)
                .description("Test description for " + title)
                .source("integration-test")
                .type("TEST")
                .build();
        return eventService.createEvent(request);
    }
}
