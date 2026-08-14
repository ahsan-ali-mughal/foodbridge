# FoodBridge

Real-time food rescue and hunger relief platform: connects restaurants, grocers, and caterers
with surplus food to nearby verified NGOs and shelters before it spoils, and coordinates
volunteer pickup/delivery through to a tax-deduction receipt.

This repo is a Maven multi-module reactor containing every FoodBridge microservice.

## Modules

| Module | What it does | Port |
|---|---|---|
| `foodbridge-common` | Shared enums, Kafka event records, exceptions, DTOs (library, no runtime) | — |
| `auth-service` | Registration, login, JWT issuance, NGO verification | 8081 |
| `listing-service` | Food listing lifecycle, geo discovery, S3 image upload | 8082 |
| `matching-service` | Geo-matches new listings to nearby NGOs | 8084 |
| `claim-service` | Concurrency-safe claim workflow (Redisson + MySQL) | 8083 |
| `logistics-service` | Volunteer pickup/delivery task tracking | 8085 |
| `notification-service` | SNS fan-out (urgent) + SQS/DLQ jobs (non-urgent) | 8086 |
| `document-service` | PDF receipt generation (PDFBox), S3 storage | 8087 |
| `admin-service` | Thymeleaf ops dashboard (NGO approvals, analytics) | 8088 |
| `api-gateway` | Single public entry point, JWT verification, rate limiting | 8080 |

Every module has its own `README.md` with service-specific detail — this file covers the system
as a whole.

## Tech stack

Java 21 · Spring Boot 3.3.4 · Spring Cloud 2023.0.3 · MySQL 8.4 · MongoDB 7 · Redis 7 + Redisson
3.37.0 · Apache Kafka · AWS SQS/SNS/S3 (LocalStack locally) · Quartz Scheduler (MySQL JobStore) ·
Thymeleaf · Spring Security (JWT) · Flyway · Docker

All dependency versions are pinned in the root `pom.xml`'s `<properties>` to specific, currently
stable, non-vulnerable releases rather than left to transitive resolution — see that file for the
full list.

## Architecture at a glance

```
Client / admin browser
        |
   [api-gateway] --JWT verify, rate limit--
        |
  -----------------------------------------------------------------
  | auth | listing | matching | claim | logistics | document | ... |
  -----------------------------------------------------------------
    |        |          |         |        |            |
  MySQL   MongoDB    MongoDB   MySQL+   MySQL       MongoDB+S3
                                Redis
        \        |                    /        \
         \-------[ Kafka ]-----------/          [ Quartz-scheduled SQS poll ]
                    |
         [notification-service] --SNS--> SMS / Email / Push (fan-out)
                    |
         [admin-service] (Thymeleaf dashboard, own login)
```

Full request/event flow for one donation:

1. Donor creates a listing → `listing-service` saves it (MongoDB) → publishes `donation.created`
2. `matching-service` consumes it, geo-queries nearby NGOs → publishes `donation.matched`
3. `notification-service` consumes the match → SNS fan-out alerts the matched NGOs
4. An NGO calls `claim-service` → Redisson lock + MySQL unique constraint guarantee exactly one
   claim succeeds → publishes `donation.claimed`
5. `logistics-service` consumes the claim → creates a pickup task; a volunteer accepts and later
   marks it delivered → publishes `pickup.completed`
6. `document-service` consumes that → generates a PDF receipt (PDFBox) → stores it in S3
7. `claim-service` also consumes `pickup.completed` → marks the claim `COMPLETED`
8. `admin-service` consumes every step above into a read-model for the ops dashboard

A correlation id, generated at `api-gateway` (or by whichever service first sees a request),
propagates through HTTP headers, Feign calls, and the `EventEnvelope` on every Kafka message —
so this entire flow for one donation is traceable by a single id across every service's logs.

## Scheduled jobs: Quartz, not `@Scheduled`

`listing-service` (expiry sweep), `notification-service`, and `document-service` (SQS pollers)
all run their periodic work as Quartz jobs backed by a **dedicated MySQL schema per service**
(`foodbridge_listing_quartz`, `foodbridge_notification_quartz`, `foodbridge_document_quartz`) —
see `mysql-init/01-create-databases.sql` and each service's `quartz/QuartzDataSourceConfig`. This
buys durable trigger state across restarts and safe horizontal scaling (only one running instance
fires a given trigger), which a Spring `@Scheduled` method cannot provide on its own.

## Running the full stack

```bash
cp .env.example .env
# edit .env and set JWT_SECRET, e.g.:
echo "JWT_SECRET=$(openssl rand -base64 32)" > .env

docker compose up -d --build
```

This brings up MySQL, MongoDB, Redis, Kafka+Zookeeper, and LocalStack, then all nine FoodBridge
services. LocalStack auto-provisions its S3 buckets, SQS queues (+ DLQs with redrive policies),
and SNS topic on first startup via `localstack/init-aws.sh` — no manual AWS setup step.

All public traffic should go through `http://localhost:8080` (api-gateway). admin-service's
dashboard is separately reachable at `http://localhost:8088/login` since it's a distinct
browser-facing app with its own session-based login.

### Building without Docker

```bash
./mvnw clean install                 # builds every module, foodbridge-common first
./mvnw -pl auth-service spring-boot:run   # run one service directly against local infra
```

> **Note on this environment:** the sandbox this repository was authored in has network access to
> npm/PyPI/crates/GitHub registries but not Maven Central, so the Maven build itself was not
> executed here. Dependency versions and code were written carefully by hand, but run a full
> `./mvnw clean install` yourself as the first step after cloning to catch anything that needs
> adjusting for your environment.

## Cross-cutting standards applied to every service

- `GlobalExceptionHandler` mapping a shared exception hierarchy (`foodbridge-common`) to
  consistent HTTP status codes and a shared `ApiError` response shape
- `CorrelationIdFilter` (or the gateway's reactive equivalent) generating/propagating a
  correlation id via MDC, HTTP headers, Feign calls, and Kafka event envelopes
- Profile-based logging: human-readable console locally/`docker`, structured JSON
  (`logstash-logback-encoder`, tagged with the service name) in `prod`
- Multi-stage Dockerfiles, non-root container users
- Java 21 records for every DTO and Kafka event contract; enums for every fixed status/role set
- Flyway-owned schema (`ddl-auto: validate`, never Hibernate auto-DDL) for every MySQL-backed service

## Documentation

Three companion documents (delivered separately, not in this repo) cover the product and
business side of FoodBridge in more depth: a Technical PRD, a Business Requirements Document, and
a Developer Understanding Document with architecture diagrams and event contract tables. This
repo is the implementation described in the third of those.

## Known simplifications (by design, for an MVP scope)

- No native mobile app; web-responsive only
- No payment processing — the platform is non-commercial
- `document-service`'s email/SMS sending in `notification-service` is simulated (logged, not
  actually dispatched) — swap in a real provider SDK where noted in that service's README
- Single-region deployment assumed; no multi-region Kafka/MySQL replication configured
- `api-gateway`'s docker-profile route list omits rate-limiter filters present in the default
  profile (see that service's README for why, and how to fix it for a real rollout)
