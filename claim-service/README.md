# claim-service

The platform's consistency-critical core: guarantees that exactly one NGO can successfully
claim a given listing, even under concurrent claim attempts.

## Responsibilities

- Accept a claim attempt from an NGO for a listing (`POST /api/claims/{listingId}`)
- Guarantee at most one successful claim per listing under concurrency
- Notify listing-service that the listing is now claimed
- Publish `donation.claimed` for downstream consumers (logistics-service, notification-service, admin-service)
- Consume `pickup.completed` from logistics-service and mark the claim COMPLETED
- Allow an NGO to cancel its own claim (`DELETE /api/claims/{claimId}`)

## Tech

Spring Boot 3.3.4 · Redisson 3.37.0 (distributed lock) · Spring Data JPA · MySQL 8 · Flyway ·
Spring Kafka (producer + consumer) · Spring Cloud OpenFeign (sync call to listing-service) ·
Java 21 records for DTOs and events

## The concurrency guarantee, in detail

Two independent layers protect against a double-claim (see `ClaimServiceImpl`):

1. **Redisson distributed lock**, keyed `lock:listing:{listingId}`, held only for the duration
   of the claim's check-then-act sequence (`tryLock(2s wait, 5s lease)`). This is the primary,
   cheap defense: it serializes concurrent claim attempts for the same listing so only one
   proceeds at a time. A contended attempt fails fast with HTTP 409 `LISTING_CONTENDED` rather
   than queueing indefinitely.

2. **MySQL unique constraint** on `claims.listing_id`. This is the safety net for the rare case
   the lock is bypassed — e.g. a node crash between acquiring the lock and its lease expiring, or
   a brief Redis failover window. A `DataIntegrityViolationException` from this constraint is
   caught and translated into the same `ALREADY_CLAIMED` conflict response a normal contended
   claim would produce — from the API consumer's point of view, both failure paths look identical.

This "lock for the common case, constraint for the edge case" pattern is deliberate: distributed
locks alone are not a substitute for a database-level invariant when the cost of getting it wrong
(two NGOs shopping up for the same food) is high.

## Inter-service communication

- **Sync (OpenFeign) → listing-service**: validates the listing is still claimable before
  attempting the claim, and notifies listing-service after a successful claim. The caller's JWT
  is relayed on this call (`FeignPropagationConfig`) so listing-service authorizes it as the same
  NGO principal.
- **Async (Kafka) →**: publishes `donation.claimed`; consumes `pickup.completed` to close the loop
  on a claim's lifecycle.

## Running locally

```bash
docker compose up mysql redis kafka zookeeper auth-service listing-service claim-service
```

Swagger UI: `http://localhost:8083/swagger-ui.html`

## Testing

Beyond unit tests, a concurrency test suite exercises `claimListing` from multiple threads
against a single listing id (using Testcontainers for MySQL + a local Redisson instance) and
asserts exactly one thread succeeds — this is the test that actually proves the guarantee this
service exists to provide; don't remove it in refactors without replacing it with an equivalent.

## Logging

Same structured-logging pattern as every other service — human-readable locally, JSON
(`"service":"claim-service"`) in `prod`, correlation id always in the MDC. Claim outcomes
(success, contended, already-claimed, expired) are logged at `INFO`/`WARN` specifically because
this is the flow you'll want to audit first when investigating a donation dispute.
