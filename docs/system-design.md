# SparkCore System Design & Architecture

This document describes the architectural decisions, scaling strategy, and failure handling mechanisms simulated in the SparkCore backend. While explicitly deployed as a **Modular Monolith** for simplicity, the application boundaries are designed to mimic a distributed microservices environment.

## 1. Scaling Strategy

### Stateless Authentication (JWT)
Stateful sessions require session replication or sticky sessions, both of which hinder horizontal scaling. SparkCore utilizes **stateless JWTs** for authentication. We can spin up $N$ instances of the `SparkcoreBackendApplication` container behind a load balancer, and any node can authenticate any incoming request instantly by verifying the JWT signature, without touching a database.

### Distributed Rate Limiting (Redis + Bucket4j)
To prevent rate limits from being isolated per-node, SparkCore uses **Redis-backed Bucket4j**. When a user attempts to log in, the token bucket state is maintained in the central Redis cache. If an attacker spams 10 requests that are routed across 3 different backend nodes, the Redis backend accurately tracks the global request count and successfully triggers a `429 Too Many Requests` response.

## 2. Distributed Transaction Safety

### The Double Spend Problem
In banking, two concurrent transfer requests for the same account could read the same initial balance, apply a deduction, and overwrite each other, effectively "creating" money or missing a deduction. This is a classic race condition.

### Solution: Optimistic Locking
SparkCore implements **Optimistic Locking** using JPA's `@Version` mechanism on the `Account` entity. 
- Every account row has a `version` number.
- When an `AccountService` thread reads the balance, it also reads the version.
- Upon saving the deducted balance, the SQL generated is effectively: `UPDATE accounts SET balance = ?, version = version + 1 WHERE id = ? AND version = ?`.
- If another thread has already modified the account, the version will have changed, the `UPDATE` affects 0 rows, and Hibernate throws an `ObjectOptimisticLockingFailureException`. 
- The REST API intercepts this and returns an `HTTP 409 Conflict`, securely preventing the double spend.

## 3. Failure Handling & Event-Driven Auditing

### Tight Coupling Vulnerability
If an `AuditLogService` writes directly to the database synchronously during a money transfer, any failure or latency spike in the audit system would block or fail the core banking transaction. 

### Solution: Apache Kafka (Asynchronous Auditing)
SparkCore decouples auditing using an **Event-Driven Architecture (EDA)** via Apache Kafka.
- When a transaction succeeds, the `AccountService` only produces a `TransactionEvent` to a Kafka topic. It does not wait for the audit log to be written.
- The `AuditLogService` acts as an independent consumer, polling the topic at its own pace.

### Consumer Resilience & Semantics
- **Offset Tracking:** Kafka tracks the consumer offset. If the `AuditLogService` crashes, it will resume exactly where it left off upon restart.
- **Retry Strategy:** In a true production environment, failed event processing can be routed to a **Dead Letter Queue (DLQ)** for manual inspection, ensuring no audit logs are ever permanently lost while preventing poison-pill messages from blocking the partition queue.

## 4. Future Enhancements

To take this architecture to the next level (Tier 1 Banking grade), the following patterns would be implemented:
1. **Idempotency Keys:** Storing a unique client-generated key (`Idempotency-Key` header) for each `POST /transfer` request to prevent duplicate transfers caused by client-side retries after network timeouts.
2. **The Outbox Pattern:** Currently, writing the transaction to Postgres and producing the event to Kafka is a "Dual Write". If the Kafka broker goes down synchronously *after* the Postgres commit, the event is lost. An Outbox pattern would write the event to the same Postgres database atomically, and a background worker (e.g., Debezium) would tail the transaction log to guarantee at-least-once delivery to Kafka.
