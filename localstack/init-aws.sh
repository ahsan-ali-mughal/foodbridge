#!/usr/bin/env bash
# Provisions the S3 buckets, SQS queues (with DLQ redrive policies), and SNS
# topic + subscriptions FoodBridge needs, against LocalStack. Mounted into
# the LocalStack container at /etc/localstack/init/ready.d/ (see
# docker-compose.yml) so it runs automatically once LocalStack is ready —
# no manual setup step required for local development.
set -euo pipefail

REGION="us-east-1"
ENDPOINT="http://localhost:4566"

echo "== FoodBridge LocalStack bootstrap =="

# ---------- S3 buckets ----------
awslocal s3 mb "s3://foodbridge-listing-images" --region "$REGION" || true
awslocal s3 mb "s3://foodbridge-ngo-verification-docs" --region "$REGION" || true
awslocal s3 mb "s3://foodbridge-receipts" --region "$REGION" || true
echo "S3 buckets created."

# ---------- SQS: notification-service ----------
awslocal sqs create-queue --queue-name notification-job-dlq --region "$REGION"
DLQ_ARN=$(awslocal sqs get-queue-attributes \
  --queue-url "$ENDPOINT/000000000000/notification-job-dlq" \
  --attribute-names QueueArn --region "$REGION" \
  --query "Attributes.QueueArn" --output text)

awslocal sqs create-queue --queue-name notification-job-queue --region "$REGION" \
  --attributes "{\"RedrivePolicy\":\"{\\\"deadLetterTargetArn\\\":\\\"${DLQ_ARN}\\\",\\\"maxReceiveCount\\\":\\\"3\\\"}\",\"VisibilityTimeout\":\"30\"}"
echo "notification-job-queue (+ DLQ) created, maxReceiveCount=3."

# ---------- SQS: document-service ----------
awslocal sqs create-queue --queue-name receipt-generation-dlq --region "$REGION"
RECEIPT_DLQ_ARN=$(awslocal sqs get-queue-attributes \
  --queue-url "$ENDPOINT/000000000000/receipt-generation-dlq" \
  --attribute-names QueueArn --region "$REGION" \
  --query "Attributes.QueueArn" --output text)

awslocal sqs create-queue --queue-name receipt-generation-queue --region "$REGION" \
  --attributes "{\"RedrivePolicy\":\"{\\\"deadLetterTargetArn\\\":\\\"${RECEIPT_DLQ_ARN}\\\",\\\"maxReceiveCount\\\":\\\"3\\\"}\",\"VisibilityTimeout\":\"60\"}"
echo "receipt-generation-queue (+ DLQ) created, maxReceiveCount=3."

# ---------- SNS: urgent alerts fan-out ----------
awslocal sns create-topic --name urgent-alerts-topic --region "$REGION"
TOPIC_ARN=$(awslocal sns list-topics --region "$REGION" \
  --query "Topics[?ends_with(TopicArn, ':urgent-alerts-topic')].TopicArn" --output text)

# A dedicated SQS queue standing in for "the SMS gateway" and another for
# "the email service" — in real AWS these would be genuine SNS subscribers
# (an SMS provider's endpoint, an email-sending Lambda, a mobile push
# endpoint); locally we fan out to SQS queues so the flow can be inspected
# and tested end-to-end without external dependencies.
awslocal sqs create-queue --queue-name urgent-alerts-sms-relay --region "$REGION"
SMS_QUEUE_ARN=$(awslocal sqs get-queue-attributes \
  --queue-url "$ENDPOINT/000000000000/urgent-alerts-sms-relay" \
  --attribute-names QueueArn --region "$REGION" --query "Attributes.QueueArn" --output text)
awslocal sns subscribe --topic-arn "$TOPIC_ARN" --protocol sqs --notification-endpoint "$SMS_QUEUE_ARN" --region "$REGION"

awslocal sqs create-queue --queue-name urgent-alerts-email-relay --region "$REGION"
EMAIL_QUEUE_ARN=$(awslocal sqs get-queue-attributes \
  --queue-url "$ENDPOINT/000000000000/urgent-alerts-email-relay" \
  --attribute-names QueueArn --region "$REGION" --query "Attributes.QueueArn" --output text)
awslocal sns subscribe --topic-arn "$TOPIC_ARN" --protocol sqs --notification-endpoint "$EMAIL_QUEUE_ARN" --region "$REGION"

echo "urgent-alerts-topic created with SMS + email relay subscriptions."
echo "== FoodBridge LocalStack bootstrap complete =="
