package com.sparkcore.backend.model;

import jakarta.persistence.*;
import java.time.Instant;

/**
 * Speichert den Ergebnis einer bereits ausgeführten Überweisung.
 * Falls derselbe Client mit dem gleichen Idempotency-Key erneut anfrägt,
 * wird die gecachte Antwort zurückgegeben – ohne die Überweisung nochmal auszuführen.
 */
@Entity
@Table(
    name = "idempotency_keys",
    uniqueConstraints = @UniqueConstraint(
        name = "uq_idempotency_key_user",
        columnNames = {"idempotency_key", "username"}
    )
)
public class IdempotencyKey {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "idempotency_key", nullable = false)
    private String idempotencyKey;

    @Column(nullable = false)
    private String username;

    @Column(name = "response_body", nullable = false)
    private String responseBody;

    @Column(name = "response_status", nullable = false)
    private int responseStatus;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    public IdempotencyKey() {}

    public IdempotencyKey(String idempotencyKey, String username, String responseBody, int responseStatus) {
        this.idempotencyKey = idempotencyKey;
        this.username = username;
        this.responseBody = responseBody;
        this.responseStatus = responseStatus;
        this.createdAt = Instant.now();
    }

    public Long getId() { return id; }
    public String getIdempotencyKey() { return idempotencyKey; }
    public String getUsername() { return username; }
    public String getResponseBody() { return responseBody; }
    public int getResponseStatus() { return responseStatus; }
    public Instant getCreatedAt() { return createdAt; }
}
