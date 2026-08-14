# auth-service

Owns identity for the FoodBridge platform: registration, login, JWT issuance/refresh, and
NGO verification. Every other service trusts the JWT this service issues rather than calling
back into it on every request.

## Responsibilities

- Register Donor / NGO / Volunteer / Admin accounts (`POST /api/auth/register`)
- Authenticate and issue short-lived access + longer-lived refresh JWTs (`POST /api/auth/login`)
- Refresh an access token from a valid refresh token (`POST /api/auth/refresh`)
- Admin verification workflow for NGOs (`PATCH /api/admin/users/{id}/verification`)
- Publish `user.registered` domain events to Kafka for downstream consumers (notification-service, admin-service)
- Expose a read-only profile lookup for other services (`GET /api/users/{id}`)

## Tech

Spring Boot 3.3.4 · Spring Security (stateless JWT, no sessions) · Spring Data JPA · MySQL 8 ·
Flyway · Spring Kafka · springdoc-openapi · Lombok · jjwt 0.12.6 · Java 21 records for all DTOs and events

## Data model

Single `users` table (see `src/main/resources/db/migration/V1__create_users_table.sql`), migrated
by Flyway on startup. Hibernate's `ddl-auto` is set to `validate` — schema changes must go through
a new Flyway migration, never through Hibernate auto-DDL.

## Security design

- Passwords hashed with BCrypt, strength 12.
- Access tokens are short-lived (default 15 min); refresh tokens default to 7 days.
- JWT claims include `role` and `verified` so downstream services can authorize most requests
  without a synchronous call back here — see `JwtAuthenticationFilter`.
- `security.jwt.secret` **must** be overridden via the `JWT_SECRET` environment variable in every
  non-local environment. The default in `application.yml` is a placeholder and intentionally
  unfit for anything but local dev.

## Events published

| Topic | Payload | Consumers |
|---|---|---|
| `user.registered` | `EventEnvelope<UserRegisteredEvent>` | notification-service, admin-service |

Kafka publish failures are logged at `ERROR` but never roll back the registration transaction —
account creation is the source of truth; event delivery is best-effort with loud logging for
alerting/reconciliation (see `AuthServiceImpl.publishUserRegistered`).

## Running locally

```bash
# from the repo root, with the full stack:
docker compose up mysql kafka zookeeper auth-service

# or standalone against a local MySQL/Kafka:
export JWT_SECRET=$(openssl rand -base64 32)
./mvnw -pl auth-service -am spring-boot:run
```

Swagger UI: `http://localhost:8081/swagger-ui.html`
Health probe: `http://localhost:8081/actuator/health`

## Logging

Local/docker profiles log human-readable lines with the correlation id inline. The `prod` profile
switches to JSON (`logstash-logback-encoder`) via `logback-spring.xml`, tagged with
`"service":"auth-service"` so logs are queryable by service and `correlationId` in your log
aggregator of choice.

## Testing

`src/test` uses Testcontainers to spin up a real MySQL instance for repository/integration tests
rather than mocking the database — this catches Flyway migration and JPA mapping issues that an
H2-based test suite would miss.
