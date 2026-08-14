# matching-service

Maintains lightweight NGO location profiles and, on every new listing, computes the set of
nearby NGOs eligible to see and claim it.

## Responsibilities

- Consume `user.registered`, and create an (unlocated) NGO profile stub for `role=NGO` accounts
- Let an NGO set/update their pickup location and service radius (`PUT /api/matching/ngo-profile/{ngoId}/location`)
- Consume `donation.created`, geo-query nearby NGO profiles, and publish `donation.matched`

## Tech

Spring Boot 3.3.4 · MongoDB (2dsphere geo index) · Spring Kafka (consumer + producer) · Java 21 records

## Why a separate NGO profile store here, instead of calling auth-service?

matching-service's hot path (`donation.created` → nearby NGOs) needs a geo query, which is not
something auth-service's relational `users` table is built for. Rather than adding geo columns to
a service that has nothing to do with geography, matching-service keeps its own minimal projection
(`ngo_profiles`: id, location, service radius) populated reactively from `user.registered` events.
auth-service remains the single source of truth for NGO *identity and verification*; matching-service
only ever needs to know *where*.

## Data flow

```
auth-service --user.registered--> matching-service (creates NgoProfile stub)
NGO calls PUT /ngo-profile/{id}/location --> matching-service (fills in location)
listing-service --donation.created--> matching-service --geo query--> donation.matched
                                                                            |
                                                            claim-service, notification-service
```

## Security note

This service's only public write endpoint (`PUT /ngo-profile/{ngoId}/location`) is intended to sit
behind the API Gateway, which authenticates the caller and rewrites `{ngoId}` from the gateway's
verified JWT rather than trusting a client-supplied path variable directly — see api-gateway's
`JwtAuthenticationFilter` and route configuration. matching-service itself does not re-validate
JWTs; it is not designed to be reachable directly from the public internet.

## Running locally

```bash
docker compose up mongo kafka zookeeper auth-service listing-service matching-service
```

## Logging

Every Kafka listener puts the event's `correlationId` into the MDC before processing so a single
donation's journey (`donation.created` → `donation.matched` → `donation.claimed` → ...) can be
traced across every service's logs by that one id.

## Testing

Repository tests exercise the 2dsphere geo query against a real MongoDB via Testcontainers.
Listener tests use an embedded/test Kafka broker to assert `donation.matched` is published with
the expected NGO id set for a variety of NGO location layouts (none nearby, one nearby, many nearby
capped at `MAX_MATCHED_NGOS`).
