package com.sparkcore.backend.model;

import jakarta.persistence.*;
import java.time.Instant;

/**
 * Outbox-Tabellen-Eintrag für das Transactional Outbox Pattern.
 *
 * Statt Kafka-Events direkt im AccountService zu feuern (was bei einem
 * Kafka-Ausfall die Buchung bestätigte, aber das Audit-Event verlor),
 * schreiben wir den Event innerhalb derselben @Transactional-Transaktion
 * in diese Tabelle. Ein @Scheduled background job liest und publiziert sie.
 *
 * Garantie: "At-least-once delivery" — self-consistent mit dem Transfer.
 */
@Entity
@Table(name = "outbox_events")
public class OutboxEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "event_type", nullable = false, length = 64)
    private String eventType;

    /** JSON-serialisiertes TransactionEvent */
    @Column(nullable = false, columnDefinition = "TEXT")
    private String payload;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    /** Null = noch nicht veröffentlicht; gesetzt nachdem Kafka bestätigt hat */
    @Column(name = "published_at")
    private Instant publishedAt;

    protected OutboxEvent() {}

    public OutboxEvent(String eventType, String payload) {
        this.eventType   = eventType;
        this.payload     = payload;
        this.createdAt   = Instant.now();
        this.publishedAt = null; // wird vom Publisher gesetzt
    }

    public Long    getId()          { return id; }
    public String  getEventType()   { return eventType; }
    public String  getPayload()     { return payload; }
    public Instant getCreatedAt()   { return createdAt; }
    public Instant getPublishedAt() { return publishedAt; }

    public void markPublished() {
        this.publishedAt = Instant.now();
    }
}
