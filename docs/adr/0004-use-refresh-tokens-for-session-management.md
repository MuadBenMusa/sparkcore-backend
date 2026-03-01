# 0004. Use Dual Token System (Access + Refresh)

* Status: accepted
* Date: 2026-03-01

## Context and Problem Statement

Stateless JWT (JSON Web Tokens) are efficient but lack a built-in revocation mechanism. If an attacker intercepts a JWT, they can use it until it expires. In banking, security requirements dictate that users must be able to securely log out, instantly invalidating their sessions, and token exposure windows must be minimized.

## Considered Options

* **Short-lived JWTs (e.g., 5 mins) requiring frequent logins:** Very secure, but creates a terrible User Experience (UX).
* **Long-lived JWTs (e.g., 24 hours):** Good UX, but highly insecure. If stolen, the token is dangerous for hours.
* **Dual Token Architecture (Access Token + Refresh Token) with Redis Blacklist:** Issue a short-lived (15 min) Access Token and a long-lived (7 day) Refresh Token. Upon Logout, immediately blacklist the Access Token in Redis and revoke the Refresh Token in PostgreSQL.

## Decision Outcome

Chosen option: **Dual Token Architecture with Redis Blacklist**. This provides the perfect balance between the scalability of stateless sessions and the strict security requirements of a banking backend.

### Positive Consequences

* **Minimized Attack Surface:** An intercepted Access Token is only valuable for a maximum of 15 minutes.
* **True Logout:** The Redis Token Blacklist allows us to forcibly kill an Access Token before its expiration, a critical feature for banking apps.
* **Token Rotation:** Every time a user requests a new Access Token using their Refresh Token, the Refresh Token itself is also rotated (replaced), mitigating replay attacks.

### Negative Consequences

* **Complexity:** Authenticating now requires checking both the JWT signature and the high-speed Redis Blacklist on every request.
* **Database State:** We had to re-introduce a minimal amount of state (Refresh Tokens) into the PostgreSQL database.
