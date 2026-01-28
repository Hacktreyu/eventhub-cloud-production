package com.eventhub.api.dto;

import com.eventhub.api.entity.Event;
import com.eventhub.api.entity.EventStatus;
import lombok.*;
import java.time.LocalDateTime;

/**
 * DTO for event response.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EventResponse {

    private Long id;
    private String title;
    private String description;
    private String source;
    private String type;
    private EventStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime processedAt;
    private Integer retryCount;

    /**
     * Convert Entity to DTO
     */
    public static EventResponse fromEntity(Event event) {
        return EventResponse.builder()
                .id(event.getId())
                .title(event.getTitle())
                .description(event.getDescription())
                .source(event.getSource())
                .type(event.getType())
                .status(event.getStatus())
                .createdAt(event.getCreatedAt())
                .processedAt(event.getProcessedAt())
                .retryCount(event.getRetryCount())
                .build();
    }
}
