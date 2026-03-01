# 0002. Use Redis and Bucket4j for Distributed Rate Limiting

* Status: accepted
* Date: 2026-03-01

## Context and Problem Statement

Banking APIs are prime targets for malicious bots aiming to brute-force authentication endpoints (`/auth/login`) or scrape data. We needed a Rate Limiting strategy to restrict IP addresses from submitting too many concurrent requests. The initial implementation utilized a Java `ConcurrentHashMap` stored in memory.

## Considered Options

* **In-Memory Map (`ConcurrentHashMap`):** Fast, but fails in a multi-instance deployment. A bot could hit Server A five times, then hit Server B five times, bypassing the global limit.
* **Database-backed Rate Limiting (PostgreSQL):** Writing and checking token buckets into a relational database creates massive transaction overhead and lock contention.
* **Redis with Bucket4j:** Redis is an in-memory key-value store optimized for ultra-fast atomic operations. Bucket4j provides a highly advanced Token Bucket algorithm.

## Decision Outcome

Chosen option: **Redis with Bucket4j**, because Rate Limits must be instantly synchronized globally across all load-balanced API instances without degrading performance.

### Positive Consequences

* **Global Protection:** If an IP is blocked on Node A, it is instantly blocked on Node B and C.
* **Negligible Latency:** Redis operates in sub-milliseconds, meaning auth request latency remains unnoticeable to legitimate users.
* **Atomic Accuracy:** Lettuce proxy managers ensure that token consumption is thread-safe and atomic across network leaps.

### Negative Consequences

* **Infrastructural Dependency:** The backend now fundamentally requires a running Redis instance to start and function correctly.
