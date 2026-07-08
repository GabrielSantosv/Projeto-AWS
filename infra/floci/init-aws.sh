#!/usr/bin/env sh
set -eu

AWS_ENDPOINT="${AWS_ENDPOINT:-http://floci:4566}"
AWS_REGION="${AWS_REGION:-us-east-1}"
ACCOUNT_ID="${ACCOUNT_ID:-000000000000}"
MAX_RECEIVE_COUNT="${MAX_RECEIVE_COUNT:-5}"

aws_cli() {
  aws --endpoint-url "$AWS_ENDPOINT" --region "$AWS_REGION" "$@"
}

topic_arn="$(aws_cli sns create-topic \
  --name saga-events-topic \
  --query TopicArn \
  --output text)"

inventory_dlq_url="$(aws_cli sqs create-queue \
  --queue-name inventory-service-dlq \
  --query QueueUrl \
  --output text)"

inventory_dlq_arn="$(aws_cli sqs get-queue-attributes \
  --queue-url "$inventory_dlq_url" \
  --attribute-names QueueArn \
  --query 'Attributes.QueueArn' \
  --output text)"

inventory_queue_url="$(aws_cli sqs create-queue \
  --queue-name inventory-service-queue \
  --attributes "RedrivePolicy={\"deadLetterTargetArn\":\"$inventory_dlq_arn\",\"maxReceiveCount\":\"$MAX_RECEIVE_COUNT\"}" \
  --query QueueUrl \
  --output text)"

inventory_queue_arn="$(aws_cli sqs get-queue-attributes \
  --queue-url "$inventory_queue_url" \
  --attribute-names QueueArn \
  --query 'Attributes.QueueArn' \
  --output text)"

aws_cli sqs set-queue-attributes \
  --queue-url "$inventory_queue_url" \
  --attributes "Policy={\"Version\":\"2012-10-17\",\"Statement\":[{\"Effect\":\"Allow\",\"Principal\":\"*\",\"Action\":\"sqs:SendMessage\",\"Resource\":\"$inventory_queue_arn\",\"Condition\":{\"ArnEquals\":{\"aws:SourceArn\":\"$topic_arn\"}}}]}"

subscription_arn="$(aws_cli sns subscribe \
  --topic-arn "$topic_arn" \
  --protocol sqs \
  --notification-endpoint "$inventory_queue_arn" \
  --query SubscriptionArn \
  --output text)"

aws_cli sns set-subscription-attributes \
  --subscription-arn "$subscription_arn" \
  --attribute-name FilterPolicy \
  --attribute-value '{"eventType":["EmployeeValidated","InvoiceIssued","InvoiceRejected","BillingRejected"]}'

cat <<EOF
SAGA_TOPIC_ARN=$topic_arn
INVENTORY_QUEUE_URL=$AWS_ENDPOINT/$ACCOUNT_ID/inventory-service-queue
INVENTORY_DLQ_URL=$inventory_dlq_url
EOF
