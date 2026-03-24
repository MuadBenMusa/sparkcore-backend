package com.sparkcore.backend.repository;

import com.sparkcore.backend.model.OutboxEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OutboxEventRepository extends JpaRepository<OutboxEvent, Long> {

    /** Alle noch nicht an Kafka gesendeten Events – der Scheduled Publisher fragt das ab */
    List<OutboxEvent> findByPublishedAtIsNull();
}
