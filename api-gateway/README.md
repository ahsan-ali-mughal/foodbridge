# api-gateway

The platform's single public entry point. Every external client (web/mobile app, third-party
integration) talks to this service; it never talks to individual business services directly.

## Responsibilities

- Route requests to the correct backend service by path prefix
- Verify the caller's JWT **once**, at the edge (`JwtAuthenticationGlobalFilter`)
- Rewrite the verified identity into `X-User-Id` / `X-User-Role` headers for downstream services
- Generate (or pass through) the request's correlation id before anything else touches it
- Apply per-user (or per-IP, for unauthenticated requests) rate limiting via Redis
- Retry idempotent (`GET`) requests once or twice on transient `502`/`503` from a backend

## Tech

Spring Cloud Gateway (WebFlux/reactive, **not** Spring MVC) · Redis (rate limiting) · jjwt ·
Java 21

## Why WebFlux here and Spring MVC everywhere else

Spring Cloud Gateway is built on Project Reactor / Netty; it does not support the Spring MVC
(Servlet) stack the rest of the platform uses. This is the one service in FoodBridge with a
different concurrency model under the hood — worth knowing before copying patterns (like the
Servlet-based `JwtAuthenticationFilter` used in every other service) into this codebase, since
they don't apply here. See `JwtAuthenticationGlobalFilter`, a `GlobalFilter` returning `Mono<Void>`,
for the reactive equivalent.

## Routing table

| Path prefix | Routes to | Public? |
|---|---|---|
| `/api/auth/register`, `/login`, `/refresh` | auth-service | Yes |
| `/api/auth/**` (other), `/api/users/**`, `/api/admin/users/**` | auth-service | No |
| `/api/listings/**` | listing-service | No |
| `/api/matching/**` | matching-service | No |
| `/api/claims/**` | claim-service | No |
| `/api/logistics/**` | logistics-service | No |
| `/api/documents/**` | document-service | No |

admin-service and notification-service are intentionally **not** routed here: admin-service is a
separate browser-facing app with its own login (see its README), and notification-service has no
public API surface — it's driven entirely by Kafka/SQS.

## Why claim-service's rate limit is tighter

`/api/claims/**` gets a noticeably lower `replenishRate`/`burstCapacity` than other routes. This
is deliberate: claim-service is the platform's concurrency-critical path (see its README), and
cheaply throttling naive claim-spam at the edge — before it ever reaches claim-service's Redisson
lock — reduces contention noise on the one endpoint where contention has real consequences.

## Security model

Downstream services trust `X-User-Id`/`X-User-Role` **only** when the request demonstrably came
through this gateway (network-level enforcement — e.g. business services not being reachable from
the public internet directly — is assumed at deployment time; this repo does not include that
network policy). Direct service-to-service calls (Feign) bypass the gateway and instead relay the
original bearer token, which each service still validates itself — see e.g. claim-service's
`FeignPropagationConfig`.

## Running locally

```bash
docker compose up redis auth-service listing-service claim-service logistics-service document-service api-gateway
```

All traffic then goes through `http://localhost:8080`.

## Note on the docker Compose profile

The `docker` profile's route list intentionally omits the rate-limiter filters present in the
default profile — Spring Boot's list-based properties replace rather than merge across profiles,
so redefining `spring.cloud.gateway.routes` for docker hostnames means restating filters too if
you want them in that profile. For a production rollout, externalize the routes (e.g. via Spring
Cloud Config or a single profile-independent routes file) rather than duplicating the list.
