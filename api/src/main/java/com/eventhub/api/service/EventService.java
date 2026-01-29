package com.eventhub.api.service;

import com.eventhub.api.dto.CreateEventRequest;
import com.eventhub.api.dto.EventMessage;
import com.eventhub.api.dto.EventResponse;
import com.eventhub.api.entity.Event;
import com.eventhub.api.entity.EventStatus;
import com.eventhub.api.exception.EventNotFoundException;
import com.eventhub.api.repository.EventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Servicio principal para gestión de eventos.
 * Coordina la persistencia en BD y la publicación en la cola (Kafka o in-memory según config).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EventService {

    private final EventRepository eventRepository;
    private final EventPublisher eventPublisher;
    private final SseService sseService;

    // Primero guarda en BD, luego publica. Así aunque falle la cola, tenemos registro del evento.
    @Transactional
    public EventResponse createEvent(CreateEventRequest request) {
        log.info("Creating new event: title='{}', source='{}', type='{}'",
                request.getTitle(), request.getSource(), request.getType());

        Event event = Event.builder()
                .title(request.getTitle())
                .description(request.getDescription())
                .source(request.getSource())
                .type(request.getType())
                .status(EventStatus.PENDING)
                .build();

        Event savedEvent = eventRepository.save(event);
        log.info("Event saved to database: id={}", savedEvent.getId());

        EventResponse response = EventResponse.fromEntity(savedEvent);

        // Publicar a la cola (Kafka o in-memory según configuración)
        EventMessage message = EventMessage.fromEventResponse(response);
        eventPublisher.publish(message);

        log.info("Event creation completed: id={}, kafkaEnabled={}",
                savedEvent.getId(), eventPublisher.isKafkaEnabled());

        sseService.notifyClients("event-created", response);
        return response;
    }

    @Transactional(readOnly = true)
    public List<EventResponse> getAllEvents() {
        log.debug("Fetching all events");
        return eventRepository.findAllOrderByCreatedAtDesc().stream()
                .map(EventResponse::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public EventResponse getEventById(Long id) {
        log.debug("Fetching event by id: {}", id);
        Event event = eventRepository.findById(id)
                .orElseThrow(() -> new EventNotFoundException("Event not found with id: " + id));
        return EventResponse.fromEntity(event);
    }

    @Transactional(readOnly = true)
    public List<EventResponse> getEventsByStatus(EventStatus status) {
        log.debug("Fetching events by status: {}", status);
        return eventRepository.findByStatus(status).stream()
                .map(EventResponse::fromEntity)
                .collect(Collectors.toList());
    }

    // Solo el consumer debe llamar a esto. Actualiza el estado y marca processedAt si es PROCESSED.
    @Transactional
    public EventResponse updateEventStatus(Long eventId, EventStatus status) {
        log.info("Updating event status: id={}, newStatus={}", eventId, status);

        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new EventNotFoundException("Event not found with id: " + eventId));

        event.setStatus(status);

        if (status == EventStatus.PROCESSED) {
            event.setProcessedAt(LocalDateTime.now());
        }

        Event updated = eventRepository.save(event);
        log.info("Event status updated: id={}, status={}", updated.getId(), updated.getStatus());

        sseService.notifyClients("event-updated", EventResponse.fromEntity(updated));
        return EventResponse.fromEntity(updated);
    }

    @Transactional(readOnly = true)
    public EventStats getStats() {
        long pending = eventRepository.countByStatus(EventStatus.PENDING);
        long processing = eventRepository.countByStatus(EventStatus.PROCESSING);
        long processed = eventRepository.countByStatus(EventStatus.PROCESSED);
        long failed = eventRepository.countByStatus(EventStatus.FAILED);
        long total = eventRepository.count();

        return new EventStats(total, pending, processing, processed, failed, eventPublisher.isKafkaEnabled());
    }

    public record EventStats(
            long total,
            long pending,
            long processing,
            long processed,
            long failed,
            boolean kafkaEnabled) {
    }

    @Transactional
    public void deleteAllEvents() {
        log.info("Deleting all events");
        eventRepository.deleteAll();
        log.info("All events deleted");
        sseService.notifyClients("events-cleared", null);
    }

    // Limpieza automática cada 5 horas para evitar que la demo acumule basura de pruebas
    @org.springframework.scheduling.annotation.Scheduled(fixedRate = 18000000) // 5h = 18_000_000ms
    @Transactional
    public void autoDeleteEvents() {
        log.info("Scheduled task: Deleting all events to clear old data");
        deleteAllEvents();
    }
}
