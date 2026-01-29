package com.eventhub.api;

import com.eventhub.api.controller.EventController;
import com.eventhub.api.dto.CreateEventRequest;
import com.eventhub.api.dto.EventResponse;
import com.eventhub.api.entity.EventStatus;

import com.eventhub.api.service.EventService;
import com.eventhub.api.service.SseService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Controller tests using MockMvc.
 */
@WebMvcTest(EventController.class)
class EventControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private EventService eventService;

    @MockBean
    private SseService sseService;

    @Test
    @DisplayName("POST /api/events - Should create event")
    void shouldCreateEvent() throws Exception {
        // Given
        CreateEventRequest request = CreateEventRequest.builder()
                .title("New Event")
                .description("Event description")
                .source("api-test")
                .type("TEST")
                .build();

        EventResponse response = EventResponse.builder()
                .id(1L)
                .title("New Event")
                .description("Event description")
                .source("api-test")
                .type("TEST")
                .status(EventStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .build();

        when(eventService.createEvent(any(CreateEventRequest.class))).thenReturn(response);

        // When/Then
        mockMvc.perform(post("/api/events")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.title").value("New Event"))
                .andExpect(jsonPath("$.status").value("PENDING"));
    }

    @Test
    @DisplayName("POST /api/events - Should return 400 for invalid input")
    void shouldReturn400ForInvalidInput() throws Exception {
        // Given - request without required fields
        CreateEventRequest request = CreateEventRequest.builder()
                .title("") // Empty title - should fail validation
                .build();

        // When/Then
        mockMvc.perform(post("/api/events")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("GET /api/events - Should return all events")
    void shouldGetAllEvents() throws Exception {
        // Given
        List<EventResponse> events = Arrays.asList(
                EventResponse.builder()
                        .id(1L)
                        .title("Event 1")
                        .status(EventStatus.PENDING)
                        .createdAt(LocalDateTime.now())
                        .build(),
                EventResponse.builder()
                        .id(2L)
                        .title("Event 2")
                        .status(EventStatus.PROCESSED)
                        .createdAt(LocalDateTime.now())
                        .build());

        when(eventService.getAllEvents()).thenReturn(events);

        // When/Then
        mockMvc.perform(get("/api/events"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].title").value("Event 1"))
                .andExpect(jsonPath("$[1].title").value("Event 2"));
    }

    @Test
    @DisplayName("GET /api/events/{id} - Should return event by ID")
    void shouldGetEventById() throws Exception {
        // Given
        EventResponse event = EventResponse.builder()
                .id(1L)
                .title("Test Event")
                .status(EventStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .build();

        when(eventService.getEventById(1L)).thenReturn(event);

        // When/Then
        mockMvc.perform(get("/api/events/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.title").value("Test Event"));
    }

    @Test
    @DisplayName("GET /api/events/stats - Should return statistics")
    void shouldGetStats() throws Exception {
        // Given
        EventService.EventStats stats = new EventService.EventStats(10, 3, 1, 5, 1, false);
        when(eventService.getStats()).thenReturn(stats);

        // When/Then
        mockMvc.perform(get("/api/events/stats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(10))
                .andExpect(jsonPath("$.pending").value(3))
                .andExpect(jsonPath("$.processed").value(5))
                .andExpect(jsonPath("$.kafkaEnabled").value(false));
    }
}
