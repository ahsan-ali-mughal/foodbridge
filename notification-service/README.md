# notification-service

Dispatches every notification the platform sends, split across two paths depending on urgency:
SNS fan-out for time-critical alerts, and an SQS job queue (with DLQ) for everything else.

## Responsibilities

- Consume `donation.matched` and `donation.expiring-soon` → publish to the `urgent-alerts-topic`
  SNS topic, which fans out to SMS, email, and push subscribers independently
- Consume `user.registered` and `donation.claimed` → enqueue non-urgent jobs (welcome email,
  claim confirmation) onto `notification-job-queue`
- Poll `notification-job-queue` and process jobs (via a Quartz job on a dedicated MySQL JobStore),
  relying entirely on the queue's own redrive policy for retry/DLQ behavior (no custom retry loop
  in application code)
- Log every notification attempt to `notification_logs` (MongoDB) as an audit trail

## Tech

Spring Boot 3.3.4 · MongoDB · Spring Kafka (4 consumers) · AWS SDK v2 (SNS + SQS) ·
**Quartz Scheduler with a dedicated MySQL JobStore** · Java 21 records/enums

## Scheduled jobs: Quartz, not `@Scheduled`

The SQS poll loop runs as a Quartz job (`quartz/NotificationJobPollerJob`) rather than a Spring
`@Scheduled` method, backed by its own MySQL schema (`foodbridge_notification_quartz`) via
`quartz/QuartzDataSourceConfig` — kept fully separate from this service's MongoDB business data.
As with listing-service, this means trigger state survives restarts and, if this service is ever
scaled to multiple instances, only one instance polls SQS per trigger fire instead of every
instance competing for the same messages.

## Why two delivery paths?

| | SNS fan-out | SQS job queue |
|---|---|---|
| Used for | `donation.matched`, `donation.expiring-soon` | `user.registered`, `donation.claimed` |
| Urgency | Time-critical — an NGO needs to know **now** | Not time-critical — can be a few minutes late |
| Delivery | Simultaneous to every subscribed channel | Sequential polling + processing |
| Failure handling | Per-subscription (SNS's own retry policy) | Queue redrive policy → DLQ after 3 attempts |

This mirrors the platform's general inter-service communication guidance: reach for the
lowest-latency, broadest-fan-out primitive (SNS) when something must reach multiple channels
immediately, and the queue-backed primitive (SQS) when "eventually, reliably" is good enough and
you want built-in retry/DLQ semantics without hand-rolling them.

## DLQ behavior, precisely

`notification-job-queue`'s redrive policy (provisioned in `localstack/init-aws.sh`) sets
`maxReceiveCount: 3` with `notification-job-dlq` as the target. `NotificationJobPoller` **only**
deletes a message after it processes successfully — on any exception the message is left alone,
becomes visible again after the visibility timeout, and SQS itself (not application code) moves it
to the DLQ once it's been received 3 times without being deleted. There is deliberately no
try/retry loop inside the poller; the queue already provides that.

## Running locally

```bash
docker compose up mongo kafka zookeeper localstack notification-service
```

## Logging

Every listener puts the event's `correlationId` into the MDC before processing, so a single
donation's full notification trail (matched-alert → claim-confirmation) is traceable by that one
id across this service's logs, exactly like every other FoodBridge service.

## Extending this service

Swap `NotificationJobPoller.sendEmail` for a real provider SDK call (SES, SendGrid, Twilio, etc.)
when moving beyond the MVP — the queueing, retry, DLQ, and audit-log plumbing around it doesn't
need to change.
