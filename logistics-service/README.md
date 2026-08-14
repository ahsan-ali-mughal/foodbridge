# logistics-service

Tracks the physical fulfillment side of a claim: creating a pickup task, letting a volunteer
accept it, and recording its progress through to delivery (or failure).

## Responsibilities

- Consume `donation.claimed` and create an unassigned `PickupTask` (idempotent by `claimId`)
- Let a volunteer browse unassigned tasks and accept one (`GET /unassigned`, `POST /{id}/accept`)
- Track status transitions: `ASSIGNED → PICKED_UP → DELIVERED` (or `FAILED`) via `PATCH /{id}/status`
- Publish `pickup.completed` on delivery (triggers document-service's receipt generation) and
  `pickup.failed` on failure

## Tech

Spring Boot 3.3.4 · Spring Data JPA · MySQL 8 · Flyway · Spring Kafka (consumer + producer) ·
Spring Security (JWT validation) · Java 21 records/enums

## State machine

```
        donation.claimed
               |
               v
          [ASSIGNED] (unassigned: volunteer_id IS NULL)
               |  volunteer accepts
               v
          [ASSIGNED] (volunteer_id set)
               |  PATCH status=PICKED_UP
               v
          [PICKED_UP]
               |  PATCH status=DELIVERED           |  PATCH status=FAILED
               v                                    v
          [DELIVERED] --publishes pickup.completed  [FAILED] --publishes pickup.failed
```

A volunteer may only transition a task they are assigned to — enforced in
`LogisticsServiceImpl.getOwnedTaskOrThrow`, returning HTTP 403 otherwise.

## Idempotency note

`createTaskForClaim` checks for an existing task by `claimId` before inserting. Kafka's
at-least-once delivery means `donation.claimed` could be redelivered; without this check a
redelivery would create a duplicate task. The `uq_pickup_tasks_claim_id` unique constraint is the
same belt-and-suspenders pattern used in claim-service: primary defense in application logic, a
DB constraint as the backstop.

## Running locally

```bash
docker compose up mysql kafka zookeeper auth-service listing-service claim-service logistics-service
```

Swagger UI: `http://localhost:8085/swagger-ui.html`

## Logging

Every status transition is logged at `INFO` (or `WARN` for failures) with `taskId` and the acting
`volunteerId`, which is the audit trail you'd pull first if a donor or NGO disputes whether a
delivery actually happened.
