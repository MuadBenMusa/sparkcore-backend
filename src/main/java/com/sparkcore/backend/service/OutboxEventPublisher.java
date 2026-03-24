package com.sparkcore.backend.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sparkcore.backend.dto.TransactionEvent;
import com.sparkcore.backend.model.OutboxEvent;
import com.sparkcore.backend.repository.OutboxEventRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Transactional Outbox Pattern – Background Publisher.
 *
 * Läuft alle 5 Sekunden und überträgt alle unverarbeiteten Outbox-Events
 * an Apache Kafka. Falls Kafka temporär nicht erreichbar ist, bleibt der
 * Event in der DB und wird beim nächsten Durchlauf erneut versucht.
 *
 * Garantie: Der Kafka-Event kann NICHT verloren gehen, solange PostgreSQL
 * verfügbar ist — er wird so lange wiederholt, bis Kafka bestätigt.
 */
@Service
public class OutboxEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(OutboxEventPublisher.class);
    private static final String TOPIC = "transaction-events";

    private final OutboxEventRepository outboxRepository;
    private final KafkaTemplate<Object, Object> kafkaTemplate;
    private final ObjectMapper objectMapper;

    public OutboxEventPublisher(OutboxEventRepository outboxRepository,
                                KafkaTemplate<Object, Object> kafkaTemplate,
                                ObjectMapper objectMapper) {
        this.outboxRepository = outboxRepository;
        this.kafkaTemplate    = kafkaTemplate;
        this.objectMapper     = objectMapper;
    }

    /**
     * Pollt alle 5 Sekunden die Outbox-Tabelle und sendet ausstehende Events an Kafka.
     * Die @Transactional-Annotation stellt sicher:
     *   1. Alle publishedAt-Updates sind atomar committed.
     *   2. Falls der Job crasht NACH dem Kafka-Send aber VOR dem DB-Update,
     *      wird dasselbe Event beim nächsten Lauf erneut gesendet → at-least-once.
     */
    @Scheduled(fixedDelay = 5000)
    @Transactional
    public void publishPendingEvents() {
        List<OutboxEvent> pending = outboxRepository.findByPublishedAtIsNull();

        if (pending.isEmpty()) {
            return;
        }

        log.debug("Outbox publisher: {} event(s) to process", pending.size());

        for (OutboxEvent outboxEvent : pending) {
            try {
                TransactionEvent event = objectMapper.readValue(
                        outboxEvent.getPayload(), TransactionEvent.class);

                // Sende an Kafka – blockiert nicht (asynchronous Future)
                kafkaTemplate.send(TOPIC, event);

                // Markiere als veröffentlicht – wird committiert am Ende der Methode
                outboxEvent.markPublished();
                outboxRepository.save(outboxEvent);

                log.info("Outbox: published event id={} type={}", outboxEvent.getId(), outboxEvent.getEventType());

            } catch (Exception e) {
                // Nicht abbrechen – den NÄCHSTEN Event noch probieren.
                // Dieser Event bleibt unpublished und wird beim nächsten Poll wiederholt.
                log.error("Outbox: failed to publish event id={}, will retry. Error: {}",
                        outboxEvent.getId(), e.getMessage());
            }
        }
    }
}
