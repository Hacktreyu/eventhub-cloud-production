package com.eventhub.api.controller;

import com.eventhub.api.dto.CreateEventRequest;
import com.eventhub.api.dto.EventResponse;
import com.eventhub.api.entity.EventStatus;
import com.eventhub.api.service.EventService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import com.eventhub.api.service.SseService;
import org.springframework.http.MediaType;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST Controller for Event operations.
 */
@RestController
@RequestMapping("/api/events")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Events", description = "API de eventos")
@CrossOrigin(origins = "*")
public class EventController {

    private final EventService eventService;
    private final SseService sseService;

    @PostMapping
    @Operation(summary = "Crear evento", description = "Crea un evento, lo guarda en BD y lo publica en la cola")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Evento creado"),
            @ApiResponse(responseCode = "400", description = "Datos inválidos")
    })
    public ResponseEntity<EventResponse> createEvent(
            @Valid @RequestBody CreateEventRequest request) {
        log.info("POST /api/events - Creating event: title='{}'", request.getTitle());
        EventResponse response = eventService.createEvent(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    @Operation(summary = "Listar eventos")
    @ApiResponse(responseCode = "200", description = "Lista de eventos")
    public ResponseEntity<List<EventResponse>> getAllEvents() {
        log.info("GET /api/events - Fetching all events");
        List<EventResponse> events = eventService.getAllEvents();
        return ResponseEntity.ok(events);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Ver evento por ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Evento encontrado"),
            @ApiResponse(responseCode = "404", description = "Evento no encontrado")
    })
    public ResponseEntity<EventResponse> getEventById(
            @Parameter(description = "ID del evento") @PathVariable Long id) {
        log.info("GET /api/events/{} - Fetching event", id);
        EventResponse response = eventService.getEventById(id);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/status/{status}")
    @Operation(summary = "Filtrar por estado")
    @ApiResponse(responseCode = "200", description = "Eventos filtrados")
    public ResponseEntity<List<EventResponse>> getEventsByStatus(
            @Parameter(description = "Estado del evento") @PathVariable EventStatus status) {
        log.info("GET /api/events/status/{} - Fetching events by status", status);
        List<EventResponse> events = eventService.getEventsByStatus(status);
        return ResponseEntity.ok(events);
    }

    @GetMapping("/stats")
    @Operation(summary = "Estadísticas")
    @ApiResponse(responseCode = "200", description = "Estadísticas de eventos")
    public ResponseEntity<EventService.EventStats> getStats() {
        log.info("GET /api/events/stats - Fetching statistics");
        EventService.EventStats stats = eventService.getStats();
        return ResponseEntity.ok(stats);
    }

    @DeleteMapping
    @Operation(summary = "Eliminar todos los eventos")
    @ApiResponse(responseCode = "204", description = "Eventos eliminados")
    public ResponseEntity<Void> deleteAllEvents() {
        log.info("DELETE /api/events - Deleting all events");
        eventService.deleteAllEvents();
        return ResponseEntity.noContent().build();
    }

    @GetMapping(value = "/subscribe", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(summary = "Suscribirse a eventos (SSE)")
    @ApiResponse(responseCode = "200", description = "Stream de eventos")
    public SseEmitter subscribe() {
        log.info("GET /api/events/subscribe - New SSE subscription");
        return sseService.subscribe();
    }
}
