# admin-service

The only browser-facing, server-rendered service in the platform: a Thymeleaf operations
dashboard for platform administrators.

## Responsibilities

- Server-side login (proxies credentials to auth-service, keeps the resulting JWT in the
  HTTP session — never exposed to client-side JS)
- Show live counters: listings created, donations claimed, deliveries completed, users
  registered, pending NGO verifications
- List and approve/reject pending NGO verifications (proxies to auth-service's admin endpoints)
- Show a recent-activity feed built from platform-wide Kafka events

## Tech

Spring Boot 3.3.4 · Thymeleaf · Spring Security (session-based, not bearer-token — see below) ·
MongoDB (activity read-model) · Spring Kafka (4 consumers) · Spring Cloud OpenFeign (auth-service) ·
Java 21 records/enums

## Why this service authenticates differently from the rest of the platform

Every other FoodBridge service is a JSON API that accepts a bearer JWT issued by auth-service.
admin-service is the one browser-driven, server-rendered exception: an admin submits a login form,
`LoginController` exchanges those credentials with auth-service's `/api/auth/login` on the
server side, and the resulting JWT is stored **server-side in the HTTP session**
(`SessionKeys.ACCESS_TOKEN`) — never sent to the browser as a cookie-readable-by-JS or
localStorage value. `SessionJwtAuthenticationFilter` reads that session-held token on every
request to populate the Spring Security context, and `FeignTokenRelayConfig` forwards it onto
every outbound call to auth-service so admin-only endpoints there authorize correctly.

## Data model

`activity_events` (MongoDB) is a denormalized read-model built purely from Kafka events — it is
not a system of record for anything. Each owning service's own store (listing-service's Mongo,
claim-service's MySQL, etc.) remains authoritative; this collection exists only to make the
dashboard's counters and activity feed fast and simple to query.

## Events consumed

`donation.created`, `donation.claimed`, `pickup.completed`, `user.registered` — each turned into
one `ActivityEvent` row for the feed and rolled up into the dashboard's summary counters.

## Running locally

```bash
docker compose up mongo kafka zookeeper auth-service admin-service
```

Then visit `http://localhost:8088/login` and sign in with an ADMIN account (see
`auth-service`'s registration endpoint, or seed one directly).

## Logging

Same structured-logging pattern as every other service: human-readable locally/docker, JSON in
`prod`, correlation id in the MDC — including on the browser-facing requests, via
`CorrelationIdFilter`, exactly like the platform's API services.
