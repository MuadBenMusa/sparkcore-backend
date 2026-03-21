# SparkCore – System Design Document

This document describes the technical architecture, scaling strategy, failure handling, and distributed systems patterns used in SparkCore. It is intended to be read alongside the [Architecture Decision Records](/docs/adr/).

---

## Architecture Overview

SparkCore is a **modular monolith** with clear microservice boundaries. Each domain module (Auth, Accounts, Audit) is fully isolated:

- Independent service and repository layers
- Async inter-domain communication via Apache Kafka
- No shared mutable state between domains

This design makes it straightforward to extract any module into a standalone microservice without restructuring the codebase.

---

## Scaling Strategy

### Horizontal Scaling (Stateless Design)

The API is **fully stateless** — no session data is stored in-process. Every node can handle any request independently.

```
Load Balancer
     │
     ├── SparkCore Node 1 ──┐
     ├── SparkCore Node 2 ──┤──► PostgreSQL (shared)
     └── SparkCore Node 3 ──┘──► Redis (shared, distributed)
                                 └── Kafka (shared broker)
```

To scale: `docker-compose up --scale backend=3`

### Why Redis enables horizontal rate limiting
Each node shares the same Redis token bucket. Without Redis, each node would maintain its own in-memory counter — allowing a brute-force attacker to send 5 requests to Node 1, 5 to Node 2, and 5 to Node 3, bypassing a per-node limit of 5. With Redis, all nodes share one bucket per IP.

### Why Redis enables distributed logout
The JWT blacklist is stored in shared Redis, not in-memory. A logout request hitting Node 1 immediately blocks the token on Node 2 and Node 3 as well.

---

## Concurrency & Double-Spend Protection

### Layer 1: `@Transactional` (Database-Level)
Every money transfer runs inside a single ACID transaction. If any step fails (e.g. saving the recipient's balance), the entire operation is rolled back.

### Layer 2: `@Version` — Optimistic Locking (Race Condition Protection)
The `Account` entity includes a `@Version` field managed by JPA/Hibernate.

**The problem it solves:**
```
Thread A reads account balance: 100€
Thread B reads account balance: 100€
Thread A deducts 100€, saves → balance: 0€, version: 2
Thread B deducts 100€, saves → sees version mismatch → OptimisticLockException ✅
```

Without `@Version`, Thread B would overwrite Thread A's save, producing a balance of 0€ from two independent 100€ debits — a silent double-spend.

**Result:** The second concurrent request receives an `OptimisticLockException`, which Spring maps to a 409 Conflict response. The client is expected to retry.

---

## Failure Handling

### Kafka Producer Failures
If the Kafka broker is unavailable when a transfer completes, the `kafkaTemplate.send()` call fails. The transfer itself is already committed to PostgreSQL (it runs before the Kafka publish).

**Current behaviour:** The audit event is silently lost.

**Production mitigation:** The **Transactional Outbox Pattern** would solve this:
1. Write the audit event to an `outbox_events` DB table *within the same transaction* as the transfer.
2. A separate background worker (polling or CDC with Debezium) reads the outbox and publishes to Kafka.
3. Kafka consumer marks the event as processed.

This guarantees **at-least-once delivery** with no dual-write inconsistency.

> This is documented as a known trade-off. The current implementation prioritises simplicity for a portfolio context. In production, the Outbox Pattern would be implemented.

### Kafka Consumer Failures
The `AuditLogConsumer` is a Kafka listener with auto-commit. If it crashes after consuming but before writing to Postgres, the event is lost.

**Production mitigation:** Disable auto-commit and use manual acknowledgment:
```java
@KafkaListener(...)
public void consume(TransactionEvent event, Acknowledgment ack) {
    auditLogRepository.save(...);
    ack.acknowledge(); // only ack after successful write
}
```

### Database Connection Failures
HikariCP connection pool handles transient DB failures with automatic reconnection. Requests during an outage receive a 500 from the GlobalExceptionHandler (no stack traces exposed to clients).

---

## Kafka Design

| Topic | Producer | Consumer | Semantics |
|---|---|---|---|
| `transaction-events` | `AccountService` | `AuditLogConsumer` | At-least-once (current) |

### Kafka Retry Strategy
Spring Kafka's default retry is 3 attempts with exponential backoff. For production, a Dead Letter Topic (DLT) should be configured:

```yaml
spring:
  kafka:
    listener:
      ack-mode: manual
    consumer:
      enable-auto-commit: false
```

### Idempotency (Future Work)
Currently, repeated Kafka delivery of the same event would create duplicate audit log entries. Adding an `event_id` (UUID) to `TransactionEvent` and checking for duplicates before insert would make the consumer **idempotent**.

---

## Security Design

| Threat | Mitigation |
|---|---|
| Brute-force login | Bucket4j + Redis rate limiting (5 req/min per IP) |
| JWT theft | 15-minute expiry + Redis blacklist on logout |
| Refresh token replay | Token rotation on every `/auth/refresh` call |
| Concurrent double-spend | `@Transactional` + `@Version` optimistic locking |
| SQL injection | JPA parameterised queries (no raw SQL) |
| Secrets exposure | Environment variables + gitignored `application-local.yaml` |
| Stack trace leakage | `GlobalExceptionHandler` catches all exceptions |

---

## Key Metrics (Actuator Endpoints)

Spring Boot Actuator exposes Micrometer metrics at `/actuator`. In production these would be scraped by Prometheus and visualised in Grafana.

| Endpoint | What it shows |
|---|---|
| `/actuator/health` | DB, Redis, Kafka connectivity |
| `/actuator/metrics` | JVM, HTTP request latencies, HikariCP pool stats |
| `/actuator/info` | Application version and build info |
