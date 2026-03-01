# 🏛️ SparkCore Banking Architecture

![Java](https://img.shields.io/badge/Java-21-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white)
![Spring Boot](https://img.shields.io/badge/Spring_Boot-4.0.3-6DB33F?style=for-the-badge&logo=spring&logoColor=white)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-15-316192?style=for-the-badge&logo=postgresql&logoColor=white)
![Redis](https://img.shields.io/badge/redis-%23DD0031.svg?&style=for-the-badge&logo=redis&logoColor=white)
![Apache Kafka](https://img.shields.io/badge/Apache_Kafka-231F20?style=for-the-badge&logo=apache-kafka&logoColor=white)
![Docker](https://img.shields.io/badge/docker-%230db7ed.svg?style=for-the-badge&logo=docker&logoColor=white)
![Build Status](https://img.shields.io/badge/build-passing-brightgreen?style=for-the-badge)

SparkCore is a high-performance, resilient, and secure financial backend API. This project demonstrates enterprise-grade architectural patterns, strict security mechanisms, and decoupled microservice communication standards heavily used in the banking sector.

## 🎯 Why I Built This

I built SparkCore to prove that I don't just know how to write code, but how to design robust distributed systems. Banking applications face unique challenges: strict auditing requirements, high-frequency brute-force attacks, and absolute atomicity in transactions.

This project addresses those challenges by implementing:
*   **Dual-Token Security Architectures** with instant revocation.
*   **Distributed Rate Limiting** to thwart credential stuffing across clusters.
*   **Event-Driven Asynchronous Auditing** to ensure core transactions never block.
*   **Mathematical Domain Logic** (ISO 13616 German IBAN Modulo-97 validation).

---

## 🏗️ Architecture Diagram

SparkCore leverages an Event-Driven Architecture (EDA) to decouple the core transactional `AccountService` from the slower, strictly-required `AuditLogService`.

```mermaid
graph TD
    Client(Client App / Postman) -->|HTTPS/JSON| API[Spring Boot API]
    DB[(PostgreSQL)]

    subgraph SparkCore Backend Node
        API -->|Every Request| RateFilter[Rate Limit Filter]
        RateFilter -->|Only POST /auth/login| Redis[(Redis Cache)]
        RateFilter -->|Every Request| JwtFilter[JWT + Redis Filter]
        JwtFilter -->|Checks Blacklist| Redis

        JwtFilter --> Auth[Auth Controller]
        JwtFilter --> Account[Account Controller]
        JwtFilter --> AuditCtrl[AuditLog Controller]

        Auth --> AuthLogic[Auth Service]
        Account --> CoreLogic[Account Service]
        AuditCtrl --> AuditRead[AuditLog Service]

        AuthLogic -->|Writes User + Refresh Token| DB
        CoreLogic -->|Writes Transaction| DB
        AuditRead -->|Reads Logs| DB

        CoreLogic -->|Produces Async Event| Kafka[[Apache Kafka Broker]]
    end

    subgraph Independent Audit Consumer
        Kafka -->|Consumes Event| AuditConsumer[AuditLog Consumer]
        AuditConsumer -->|Persists Log| DB
    end
```

---

## 🔐 Key Architectural Decisions (ADRs)

I believe senior engineers must document their trade-offs. You can find detailed Markdown Architecture Decision Records in the [`/docs/adr/`](/docs/adr/) folder.

1.  **[ADR-0001: Statutory JWT Authentication]** - Why stateless validation scales infinitely better than server-side session stores.
2.  **[ADR-0002: Redis Distributed Rate Limiting]** - Why `ConcurrentHashMap` fails in load-balanced environments and why Lettuce+Bucket4j is superior.
3.  **[ADR-0003: Kafka Asynchronous Auditing]** - Why tight coupling transactions to audit logs destroys latency and how Event Streaming fixes it.
4.  **[ADR-0004: Dual Token Architecture]** - How issuing 7-day Refresh Tokens and 15-minute Access Tokens with a Redis Blacklist solves the JWT revocation flaw.

---

## 🚀 Features

### Security & Authentication
- **Dual-Token Flow (Access + Refresh):** Minimized attack windows. Refresh tokens are stored safely, while rotated access tokens are verified statelessly.
- **Stateful Logout (Redis):** Tokens are placed on a high-speed Redis Blacklist upon logout, rendering intercepted tokens useless instantly.
- **Global Rate Limiting:** Bucket4j intercepts malicious API spammers globally via Redis, saving database cycles.

### Core Banking Logic
- **ISO-13616 IBAN Generation:** Accounts are automatically assigned mathematically valid German IBANs utilizing Modulo-97 calculations (ISO 7064).
- **Custom Bean Validation (`@ValidIban`):** Automatically rejects transfers to fake/structurally-invalid IBANs on the HTTP layer.
- **ACID Transactions:** Full Rollback capabilities on failed transfers via Spring's declarative `@Transactional`.

### Observability & Quality
- **100% Passing Integration Tests:** `Testcontainers` spins up real ephemeral instances of PostgreSQL, Redis, and Kafka for true integration coverage.
- **Flyway Migrations:** Bulletproof, version-controlled database schemas.

---

## 📂 Project Structure

```
src/main/java/com/sparkcore/backend/
├── config/         # Spring Security, Redis, Kafka, OpenAPI bean configuration
├── controller/     # REST controllers — HTTP layer only, zero business logic
├── dto/            # Immutable Java Records for all requests & responses
├── exception/      # GlobalExceptionHandler + RefreshTokenException
├── model/          # JPA entities: AppUser, Account, Transaction, AuditLog, RefreshToken
├── repository/     # Spring Data JPA repositories
├── security/       # JwtAuthenticationFilter, LoginRateLimitFilter, JwtService
├── service/        # All business logic: AuthService, AccountService, AuditLogService...
├── util/           # IbanUtils (Modulo-97), RequestUtils (IP extraction)
└── validation/     # @ValidIban annotation + IbanValidator (Bean Validation)

src/main/resources/
├── db/migration/   # Flyway SQL migrations (V1, V2...)
├── application.yaml          # Main config (Redis, Kafka, JPA)
└── application-local.yaml    # 🔒 Gitignored — secrets go here

docs/
└── adr/            # Architecture Decision Records (4 docs)
```

## 🛠️ Getting Started

### Prerequisites
- Docker Desktop
- Java 21+

### 1. Boot Infrastructure (Database, Cache, Broker)
```bash
docker-compose up -d
```
*(Starts Postgres 15, Redis 7, and Kafka 3.8 in KRaft mode)*

### 2. Configure Local Secrets
Create a `src/main/resources/application-local.yaml` (gitignored) to store your passwords safely:
```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/sparkcore_db
    username: sparkcore_user
    password: my_secret_db_password
application:
  security:
    jwt:
      secret-key: [Your_Base64_256Bit_Secret]
```

### 3. Run the Backend
```bash
./mvnw spring-boot:run
```

---

## 📚 API interactive Documentation

Once running, explore the fully documented OpenAPI standard UI:
👉 **[http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html)**

### Core Endpoints

| Method | Endpoint | Description | Auth |
|---|---|---|---|
| `POST` | `/api/v1/auth/register` | Register a new user | Public |
| `POST` | `/api/v1/auth/login` | Returns Access (15 min) & Refresh (7 day) Token | Public |
| `POST` | `/api/v1/auth/refresh` | Rotates the Access & Refresh Tokens | Token |
| `POST` | `/api/v1/auth/logout` | Blacklists JWT in Redis & revokes Refresh Token | Token |
| `POST` | `/api/v1/accounts` | Create a bank account (IBAN auto-generated) | ADMIN |
| `GET` | `/api/v1/accounts` | List all bank accounts | ADMIN |
| `GET` | `/api/v1/accounts/{id}` | Get account by ID | Token |
| `POST` | `/api/v1/accounts/transfer` | Transfer money (validates @ValidIban, @Transactional) | USER |
| `GET` | `/api/v1/accounts/{iban}/transactions` | Get transaction history for an IBAN | Token |
| `GET` | `/api/v1/audit-logs` | Asynchronous Kafka-processed audit trail | ADMIN |
| `GET` | `/api/v1/system/ping` | Health check | Public |
