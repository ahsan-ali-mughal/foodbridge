# foodbridge-common

Shared library consumed by every FoodBridge microservice. Published as an internal Maven
artifact (`com.foodbridge:foodbridge-common`) so that event and error contracts never drift
between producer and consumer services.

## What lives here

| Package | Contents |
|---|---|
| `enums` | Platform-wide enums: `Role`, `ListingStatus`, `ClaimStatus`, `PickupStatus`, `NotificationChannel`, `VerificationStatus` |
| `event` | Kafka event contracts as immutable Java `record`s, wrapped in a generic `EventEnvelope<T>` |
| `dto` | Cross-service response shapes: `ApiError`, `PageResponse<T>` |
| `exception` | Shared exception hierarchy (`FoodBridgeException` and subtypes) mapped consistently to HTTP status codes by each service's `GlobalExceptionHandler` |
| `util` | `CorrelationIdHolder` — single source of truth for the correlation-id header/MDC/Kafka-header name |

## Design rules

- **Records only for wire contracts.** Every event and DTO is a `record` — immutable, no setters,
  free `equals`/`hashCode`/`toString`. This makes accidental mutation of a shared object impossible.
- **No framework annotations that leak a specific service's stack.** This module must stay
  usable from a Spring MVC service, a Spring WebFlux gateway, or a plain Kafka consumer alike.
- **Additive versioning only.** Never rename or remove a field on a published event `record`.
  Add new fields with sensible defaults handled by consumers; bump `EventEnvelope.version()` on
  any breaking change and coordinate a migration window across consumers first.
- **Exceptions carry a stable `errorCode`**, independent of the human-readable message, so API
  consumers can branch on `errorCode` without parsing prose.

## Consuming this module

Add to any service's `pom.xml` (already wired via the parent POM's `dependencyManagement`):

```xml
<dependency>
    <groupId>com.foodbridge</groupId>
    <artifactId>foodbridge-common</artifactId>
</dependency>
```

## Versioning & compatibility

This module is versioned in lockstep with the parent POM (`1.0.0`). Because every service
depends on the same version via the reactor build, there is no cross-version skew within a
single deployment of the monorepo. If services are ever split into separate repos, publish this
jar to a private artifact registry and pin each service's version explicitly.
