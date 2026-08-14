# listing-service

Owns the lifecycle of food donation listings: creation, geo-based discovery, expiry, and the
pre-signed image upload flow to S3.

## Responsibilities

- Create listings on behalf of a verified donor (`POST /api/listings`)
- Issue pre-signed S3 upload URLs so photos go straight from the browser to S3 (`POST /api/listings/upload-url`)
- Geo-search active listings near a point for NGO discovery (`GET /api/listings/nearby`)
- Publish `donation.created` and `donation.expiring-soon` Kafka events
- Expire stale listings and raise expiry warnings on a scheduled job (Quartz, every 60s by default)
- Expose an internal `markClaimed` path used by claim-service once a claim succeeds

## Tech

Spring Boot 3.3.4 · MongoDB (2dsphere geo index) · Spring Kafka · Spring Cloud OpenFeign
(sync call to auth-service) · AWS SDK v2 S3 presigner · Spring Security (JWT, verification only —
tokens are issued exclusively by auth-service) · **Quartz Scheduler with a dedicated MySQL JobStore**
· Java 21 records for all DTOs and events

## Scheduled jobs: Quartz, not `@Scheduled`

Listing expiry runs as a Quartz job (`quartz/ListingExpiryJob`) on a `SimpleTrigger`, rather than
a Spring `@Scheduled` method. Quartz's JDBC `JobStore` persists trigger/fire state in its own
MySQL schema — `foodbridge_listing_quartz`, entirely separate from the service's MongoDB business
data (see `quartz/QuartzDataSourceConfig`). This buys two things `@Scheduled` doesn't:

- **Survives restarts.** Trigger state (last fire time, misfire count) is durable, not in-memory.
- **Safe to scale horizontally.** With `org.quartz.jobStore.isClustered: true`, if listing-service
  is ever run as multiple instances, only one instance fires the job per trigger interval —
  `@Scheduled` would have had every instance independently (and redundantly) expiring listings.

The Quartz schema (`QRTZ_*` tables) is auto-initialized on startup via
`spring.quartz.jdbc.initialize-schema: always`.

## Data model

MongoDB was chosen over a relational table here specifically because listing shape varies a lot
by donor type, and because `$nearSphere` geo-queries against a `2dsphere` index are a natural fit
for "find listings near this NGO" — the platform's most latency-sensitive read.

`auto-index-creation: true` ensures the 2dsphere index exists on startup; in a real production
rollout you'd manage this via a migration tool (e.g. Mongock) instead of relying on Spring Data's
auto-creation.

## Inter-service communication

- **Sync (OpenFeign) → auth-service**: `UserServiceClient` resolves the donor's role before
  accepting a listing. This is a genuine "must know now" dependency, unlike the async events below.
- **Async (Kafka) →**: `donation.created` (consumed by matching-service, notification-service,
  admin-service) and `donation.expiring-soon` (consumed by notification-service).
- The correlation id from the inbound HTTP request is forwarded onto every outbound Feign call
  (`FeignCorrelationIdPropagationConfig`) so a single request can be traced end-to-end in logs.

## S3 upload flow

1. Client calls `POST /api/listings/upload-url` with a filename + content type.
2. listing-service returns a pre-signed PUT URL (5 minute TTL) plus the resulting public object URL.
3. Client uploads the image bytes directly to S3/LocalStack — the API never sees the raw bytes.
4. Client includes the returned object URL in `imageUrls` when calling `POST /api/listings`.

## Running locally

```bash
docker compose up mongo kafka zookeeper localstack auth-service listing-service
```

Swagger UI: `http://localhost:8082/swagger-ui.html`

## Logging

Same pattern as every other service: human-readable console logs locally/docker, JSON
(`logstash-logback-encoder`) tagged `"service":"listing-service"` in the `prod` profile, with
`correlationId` always present in the MDC and log line.

## Testing

Repository tests run against a real MongoDB via Testcontainers rather than an embedded fake, so
the geo-index behavior is exercised the same way it runs in production.
