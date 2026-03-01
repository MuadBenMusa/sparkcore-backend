# Architecture Decision Records (ADRs)

This directory contains the Architecture Decision Records for the SparkCore Banking Backend. 
We use ADRs to document the architectural and design choices made during the development of this application.

## What is an ADR?

An Architecture Decision Record (ADR) is a short text file in a format similar to an Alexandrian pattern that describes a set of forces and a single decision in response to those forces. They help future developers (and our future selves) understand the *why* behind the architecture, not just the *how*.

## Architecture Log

* [0001. Use JWT for Stateless Authentication](0001-use-jwt-for-stateless-authentication.md)
* [0002. Use Redis for Distributed Rate Limiting](0002-use-redis-for-distributed-rate-limiting.md)
* [0003. Use Apache Kafka for Asynchronous Audit Logging](0003-use-kafka-for-asynchronous-audit-logging.md)
* [0004. Use Dual Token System (Access + Refresh)](0004-use-refresh-tokens-for-session-management.md)
