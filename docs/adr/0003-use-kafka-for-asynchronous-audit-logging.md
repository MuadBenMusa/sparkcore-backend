# 0003. Use Apache Kafka for Asynchronous Audit Logging

* Status: accepted
* Date: 2026-03-01

## Context and Problem Statement

In the SparkCore banking backend, every sensitive action (e.g., creating accounts, transferring money) must be strictly audited. Our initial design tightly coupled the `AccountService` to the `AuditLogRepository`, meaning the core banking transaction had to wait for the audit log database insert to complete before returning a response to the user.

## Considered Options

* **Synchronous Database Writes:** Simple, transactional, but introduces direct coupling and harms API latency. If the audit table is locked or slow, the entire money transfer fails.
* **Asynchronous Internal Threads (e.g., Spring `@Async`):** Solves latency but risks data loss. If the server crashes mid-flight, the pending thread queue in RAM is destroyed, and the audit log is lost forever.
* **Event Streaming (Apache Kafka):** Offloads the event to a distributed, persistent message broker. The core service acts as a Producer, and a detached service acts as the Consumer.

## Decision Outcome

Chosen option: **Apache Kafka**, because it guarantees message durability and allows us to fully decouple the Core Banking operations from the peripheral Auditing operations.

### Positive Consequences

* **Microservices Readiness:** The system is now built for an Event-Driven Architecture. We can easily extract `AuditLogService` into its own completely separate deployment unit (Microservice) later without touching `AccountService`.
* **Zero Latency Impact:** The user receives a strict "TRANSFER SUCCESS" immediately. The background audit log writing happens independently without blocking the HTTP response thread.
* **Durability:** Kafka persists topics to disk. If the database crashes, Kafka holds the `TransactionEvent` messages until the DB recovers.

### Negative Consequences

* **Eventual Consistency:** There is a brief fractional-second gap where a transfer is completed but the corresponding audit log does not yet exist in the database.
* **Complexity:** Local development and CI pipelines now require managing a Kafka broker (via Docker / Testcontainers).
