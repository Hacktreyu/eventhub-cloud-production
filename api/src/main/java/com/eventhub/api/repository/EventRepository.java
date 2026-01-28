package com.eventhub.api.repository;

import com.eventhub.api.entity.Event;
import com.eventhub.api.entity.EventStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository for Event entity operations.
 */
@Repository
public interface EventRepository extends JpaRepository<Event, Long> {

    List<Event> findByStatus(EventStatus status);

    List<Event> findBySource(String source);

    List<Event> findByType(String type);

    @Query("SELECT e FROM Event e ORDER BY e.createdAt DESC")
    List<Event> findAllOrderByCreatedAtDesc();

    @Query("SELECT COUNT(e) FROM Event e WHERE e.status = :status")
    long countByStatus(EventStatus status);

    List<Event> findByStatusOrderByCreatedAtAsc(EventStatus status);
}
