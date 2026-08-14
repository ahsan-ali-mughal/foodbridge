# document-service

Generates the PDF donation receipt / impact certificate for each completed delivery and stores
it in S3.

## Responsibilities

- Consume `pickup.completed` → create a pending `Receipt` record and enqueue PDF generation onto
  `receipt-generation-queue`
- Poll that queue (via a Quartz job) and, for each message: fetch the listing's donor from
  listing-service, render a PDF with PDFBox, upload it to S3, and mark the receipt `GENERATED`
- Serve a pre-signed download URL for a generated receipt (`GET /api/documents/{claimId}/receipt`)

## Tech

Spring Boot 3.3.4 · MongoDB · Spring Kafka · Spring Cloud OpenFeign (sync call to listing-service) ·
AWS SDK v2 (S3 + SQS) · **Apache PDFBox 3.0.3** · **Quartz Scheduler with a dedicated MySQL JobStore** ·
Spring Security (JWT validation) · Java 21 records/enums

## Why PDFBox 3.x specifically

PDFBox 3.x is the current actively-maintained major version; known CVEs against the platform
affect the legacy 2.x line, not 3.x. Pin `<pdfbox.version>` explicitly rather than letting a
transitive dependency resolve an older release.

## Flow

```
logistics-service --pickup.completed--> document-service
                                              |
                                    create pending Receipt (Mongo)
                                              |
                                    enqueue --> receipt-generation-queue (SQS)
                                              |
                          [Quartz poll, every 5s] --> fetch donor (Feign) --> render PDF (PDFBox)
                                              |
                                    upload --> s3://foodbridge-receipts/{claimId}/receipt.pdf
                                              |
                                    mark Receipt GENERATED
```

On any failure in that last stage, the receipt is marked `FAILED` and the exception is re-thrown
so the SQS message is **not** deleted — it becomes visible again and is retried by the next poll,
eventually landing in `receipt-generation-dlq` after 3 failed attempts (the queue's own redrive
policy, not custom retry code — see localstack/init-aws.sh).

## Scheduled jobs: Quartz, not `@Scheduled`

Same pattern as listing-service and notification-service: the SQS poll loop is a Quartz job
(`quartz/ReceiptGenerationJob`) backed by its own MySQL schema (`foodbridge_document_quartz`),
kept separate from this service's MongoDB business data. Trigger state survives restarts, and
only one running instance polls per fire if this service is ever scaled horizontally.

## Idempotency

Both `createPendingReceipt` (checked by `claimId`) and `processReceiptGenerationJob` (checked by
`ReceiptStatus.GENERATED`) are safe to run twice — necessary given Kafka and SQS's at-least-once
delivery guarantees.

## Running locally

```bash
docker compose up mongo kafka zookeeper localstack listing-service document-service
```

Swagger UI: `http://localhost:8087/swagger-ui.html`
