package com.eventhub.api;

import com.eventhub.api.dto.CreateEventRequest;
import com.eventhub.api.dto.EventResponse;
import com.eventhub.api.entity.Event;
import com.eventhub.api.entity.EventStatus;
import com.eventhub.api.exception.EventNotFoundException;
import com.eventhub.api.repository.EventRepository;
import com.eventhub.api.service.EventPublisher;

import com.eventhub.api.service.EventService;
import com.eventhub.api.service.SseService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for EventService.
 */
@ExtendWith(MockitoExtension.class)
class EventServiceTest {

    @Mock
    private EventRepository eventRepository;

    @Mock
    private EventPublisher eventPublisher;

    @Mock
    private SseService sseService;

    @InjectMocks
    private EventService eventService;

    private Event testEvent;
    private CreateEventRequest createRequest;

    @BeforeEach
    void setUp() {
        testEvent = Event.builder()
                .id(1L)
                .title("Test Event")
                .description("Test Description")
                .source("test-source")
                .type("TEST")
                .status(EventStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .build();

        createRequest = CreateEventRequest.builder()
                .title("Test Event")
                .description("Test Description")
                .source("test-source")
                .type("TEST")
                .build();
    }

    @Test
    @DisplayName("Should create event successfully")
    void shouldCreateEvent() {
        // Given
        when(eventRepository.save(any(Event.class))).thenReturn(testEvent);
        when(eventPublisher.isKafkaEnabled()).thenReturn(false);

        // When
        EventResponse response = eventService.createEvent(createRequest);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getTitle()).isEqualTo("Test Event");
        assertThat(response.getStatus()).isEqualTo(EventStatus.PENDING);

        verify(eventRepository, times(1)).save(any(Event.class));
        verify(eventPublisher, times(1)).publish(any());
    }

    @Test
    @DisplayName("Should get all events")
    void shouldGetAllEvents() {
        // Given
        Event event2 = Event.builder()
                .id(2L)
                .title("Event 2")
                .source("source")
                .type("TYPE")
                .status(EventStatus.PROCESSED)
                .createdAt(LocalDateTime.now())
                .build();

        when(eventRepository.findAllOrderByCreatedAtDesc())
                .thenReturn(Arrays.asList(testEvent, event2));

        // When
        List<EventResponse> events = eventService.getAllEvents();

        // Then
        assertThat(events).hasSize(2);
        verify(eventRepository, times(1)).findAllOrderByCreatedAtDesc();
    }

    @Test
    @DisplayName("Should get event by ID")
    void shouldGetEventById() {
        // Given
        when(eventRepository.findById(1L)).thenReturn(Optional.of(testEvent));

        // When
        EventResponse response = eventService.getEventById(1L);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(1L);
    }

    @Test
    @DisplayName("Should throw exception when event not found")
    void shouldThrowExceptionWhenEventNotFound() {
        // Given
        when(eventRepository.findById(999L)).thenReturn(Optional.empty());

        // When/Then
        assertThatThrownBy(() -> eventService.getEventById(999L))
                .isInstanceOf(EventNotFoundException.class)
                .hasMessageContaining("Event not found with id: 999");
    }

    @Test
    @DisplayName("Should update event status to PROCESSED")
    void shouldUpdateEventStatus() {
        // Given
        when(eventRepository.findById(1L)).thenReturn(Optional.of(testEvent));
        when(eventRepository.save(any(Event.class))).thenAnswer(invocation -> {
            Event saved = invocation.getArgument(0);
            return saved;
        });

        // When
        EventResponse response = eventService.updateEventStatus(1L, EventStatus.PROCESSED);

        // Then
        assertThat(response.getStatus()).isEqualTo(EventStatus.PROCESSED);
        assertThat(response.getProcessedAt()).isNotNull();
        verify(eventRepository, times(1)).save(any(Event.class));
    }

    @Test
    @DisplayName("Should get events by status")
    void shouldGetEventsByStatus() {
        // Given
        when(eventRepository.findByStatus(EventStatus.PENDING))
                .thenReturn(List.of(testEvent));

        // When
        List<EventResponse> events = eventService.getEventsByStatus(EventStatus.PENDING);

        // Then
        assertThat(events).hasSize(1);
        assertThat(events.get(0).getStatus()).isEqualTo(EventStatus.PENDING);
    }

    @Test
    @DisplayName("Should return correct statistics")
    void shouldGetStats() {
        // Given
        when(eventRepository.countByStatus(EventStatus.PENDING)).thenReturn(5L);
        when(eventRepository.countByStatus(EventStatus.PROCESSING)).thenReturn(2L);
        when(eventRepository.countByStatus(EventStatus.PROCESSED)).thenReturn(10L);
        when(eventRepository.countByStatus(EventStatus.FAILED)).thenReturn(1L);
        when(eventRepository.count()).thenReturn(18L);
        when(eventPublisher.isKafkaEnabled()).thenReturn(true);

        // When
        EventService.EventStats stats = eventService.getStats();

        // Then
        assertThat(stats.total()).isEqualTo(18);
        assertThat(stats.pending()).isEqualTo(5);
        assertThat(stats.processing()).isEqualTo(2);
        assertThat(stats.processed()).isEqualTo(10);
        assertThat(stats.failed()).isEqualTo(1);
        assertThat(stats.kafkaEnabled()).isTrue();
    }
}
