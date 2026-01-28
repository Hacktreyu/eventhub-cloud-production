package com.eventhub.api.dto;

import lombok.*;
import java.time.LocalDateTime;

/**
 * Message DTO for Kafka events.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EventMessage {

    private Long eventId;
    private String title;
    private String source;
    private String type;
    private LocalDateTime timestamp;

    public static EventMessage fromEventResponse(EventResponse event) {
        return EventMessage.builder()
                .eventId(event.getId())
                .title(event.getTitle())
                .source(event.getSource())
                .type(event.getType())
                .timestamp(LocalDateTime.now())
                .build();
    }
}
