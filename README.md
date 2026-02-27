# SparkCore Banking Backend

A RESTful banking backend API built with **Java 21** and **Spring Boot 4**, demonstrating enterprise-grade architecture patterns used in the financial industry.

---

## Tech Stack

| Layer | Technology |
|---|---|
| Language | Java 21 |
| Framework | Spring Boot 4 |
| Security | Spring Security + JWT (jjwt 0.12.5) |
| Database | PostgreSQL 15 |
| ORM | Spring Data JPA / Hibernate |
| Validation | Jakarta Bean Validation |
| Documentation | SpringDoc OpenAPI / Swagger UI |
| Containerization | Docker + Docker Compose |
| Testing | JUnit 5 + Mockito |

---

## Features

- **JWT Authentication** — Stateless token-based auth with BCrypt password hashing
- **Role-Based Authorization** — `USER` and `ADMIN` roles enforced via `@PreAuthorize`
- **Account Management** — Create and query bank accounts
- **Money Transfers** — Transactional transfers with balance validation (`@Transactional`)
- **Transaction History** — Full ledger per IBAN, sorted by timestamp
- **Audit Logging** — Every sensitive action (transfers, account creation) is logged with username, IP address, and timestamp
- **Global Exception Handling** — Structured JSON error responses via `@ControllerAdvice`
- **Input Validation** — All DTOs validated with `@Valid` + constraint annotations
- **OpenAPI Docs** — Swagger UI with JWT bearer auth support

---

## Getting Started

### Prerequisites
- Docker Desktop
- Java 21
- Maven

### 1. Start the Database

```bash
docker-compose up -d
```

This starts a PostgreSQL 15 container (`sparkcore-postgres`) on port `5432`.

### 2. Configure Local Secrets

Create `src/main/resources/application-local.yaml` (never commit this file). This file contains all sensitive configuration and development-only overrides:

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/sparkcore_db
    username: sparkcore_user
    password: super_secret_password
  jpa:
    show-sql: true
    properties:
      hibernate:
        format_sql: true

application:
  security:
    jwt:
      secret-key: 404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970
```

### 3. Run the Application

```bash
./mvnw spring-boot:run
```

The API will be available at `http://localhost:8080`.

---

## API Overview

| Method | Endpoint | Role | Description |
|---|---|---|---|
| `POST` | `/api/v1/auth/register` | Public | Register a new user |
| `POST` | `/api/v1/auth/login` | Public | Login and receive JWT |
| `POST` | `/api/v1/accounts` | Auth | Create a bank account |
| `GET` | `/api/v1/accounts` | ADMIN | List all accounts |
| `GET` | `/api/v1/accounts/{id}` | Auth | Get account by ID |
| `POST` | `/api/v1/accounts/transfer` | USER | Transfer money |
| `GET` | `/api/v1/accounts/{iban}/transactions` | Auth | Transaction history |
| `GET` | `/api/v1/audit-logs` | ADMIN | View full audit trail |
| `GET` | `/api/v1/system/ping` | Public | Health check |

Full interactive documentation: [http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html)

---

## Project Structure

```
src/main/java/com/sparkcore/backend/
├── config/          # Spring Security & OpenAPI configuration
├── controller/      # REST controllers (HTTP layer only)
├── dto/             # Request/Response data transfer objects
├── exception/       # Global exception handler
├── model/           # JPA entities (Account, AppUser, Transaction, AuditLog)
├── repository/      # Spring Data JPA repositories
├── security/        # JWT filter and service
└── service/         # Business logic (AccountService, AuthService)
```

---

## Architecture Decisions

- **Layered architecture** — strict separation between Controller, Service, and Repository layers
- **Stateless sessions** — no server-side session state; every request is authenticated via JWT
- **Immutable audit records** — `AuditLog` and `Transaction` entities have no setters
- **`@Transactional` on transfers** — guarantees atomicity; if anything fails mid-transfer, the database rolls back automatically
- **Secrets via environment variables** — no credentials in source code; all secrets are injected at runtime
