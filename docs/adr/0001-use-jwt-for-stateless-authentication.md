# 0001. Use JWT for Stateless Authentication

* Status: accepted
* Date: 2026-03-01

## Context and Problem Statement

SparkCore Backend needs to authenticate users and authorize their requests to protected API endpoints. The system must be scalable, performant, and secure against common attacks. Stateful sessions (like standard `JSESSIONID`) introduce tight coupling between the user and a specific backend instance or require a shared session repository.

## Considered Options

* **Stateful Sessions (Cookies + Spring Session):** Requires sticky sessions or a centralized session store (e.g., Redis) which adds latency to every single sub-request.
* **Stateless JWT (JSON Web Tokens):** Tokens are self-contained and cryptographically signed. Any backend instance can verify the user instantly without database lookups.
* **OAuth2 / OIDC:** Overkill for a standalone backend serving a trusted first-party frontend client.

## Decision Outcome

Chosen option: **Stateless JWT (JSON Web Tokens)**, because it ensures horizontal scalability and drastically reduces database load. The backend can independently verify tokens via cryptographic signatures (HMAC SHA-384).

### Positive Consequences

* **Massive Scalability:** The backend holds zero session state. Requests can be load-balanced across 1,000 instances seamlessly.
* **Performance:** Validating the token requires CPU (fast) instead of a database hit (slow).
* **Decoupling:** Ideal for Microservices or SPA (Single Page Application) frontends.

### Negative Consequences

* **Token Revocation:** Unlike server-side sessions, a pure stateless JWT cannot be revoked before it expires. This necessitates keeping expiration times extremely short (e.g., 15 minutes) and implementing a Refresh Token architecture for session persistence.
