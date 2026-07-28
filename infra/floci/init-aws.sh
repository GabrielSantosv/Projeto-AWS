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

create_queue_with_dlq() {
  service_name="$1"
  queue_name="$2"
  filter_policy="$3"

  dlq_url="$(aws_cli sqs create-queue \
    --queue-name "$service_name-dlq" \
    --query QueueUrl \
    --output text)"
  dlq_arn="$(aws_cli sqs get-queue-attributes \
    --queue-url "$dlq_url" \
    --attribute-names QueueArn \
    --query 'Attributes.QueueArn' \
    --output text)"

  queue_url="$(aws_cli sqs create-queue \
    --queue-name "$queue_name" \
    --attributes "RedrivePolicy={\"deadLetterTargetArn\":\"$dlq_arn\",\"maxReceiveCount\":\"$MAX_RECEIVE_COUNT\"}" \
    --query QueueUrl \
    --output text)"
  queue_arn="$(aws_cli sqs get-queue-attributes \
    --queue-url "$queue_url" \
    --attribute-names QueueArn \
    --query 'Attributes.QueueArn' \
    --output text)"

  aws_cli sqs set-queue-attributes \
    --queue-url "$queue_url" \
    --attributes "Policy={\"Version\":\"2012-10-17\",\"Statement\":[{\"Effect\":\"Allow\",\"Principal\":\"*\",\"Action\":\"sqs:SendMessage\",\"Resource\":\"$queue_arn\",\"Condition\":{\"ArnEquals\":{\"aws:SourceArn\":\"$topic_arn\"}}}]}"

  subscription_arn="$(aws_cli sns list-subscriptions-by-topic \
    --topic-arn "$topic_arn" \
    --query "Subscriptions[?Endpoint=='$queue_arn'].SubscriptionArn | [0]" \
    --output text)"

  if [ -z "$subscription_arn" ] || [ "$subscription_arn" = "None" ]; then
    subscription_arn="$(aws_cli sns subscribe \
      --topic-arn "$topic_arn" \
      --protocol sqs \
      --notification-endpoint "$queue_arn" \
      --query SubscriptionArn \
      --output text)"
  fi

  aws_cli sns set-subscription-attributes \
    --subscription-arn "$subscription_arn" \
    --attribute-name FilterPolicy \
    --attribute-value "$filter_policy"

  printf '%s queue=%s dlq=%s filter=%s\n' "$service_name" "$queue_name" "$service_name-dlq" "$filter_policy"
}

# Cada consumidor possui uma fila e DLQ próprias. O atributo eventType é
# publicado pelos serviços e é usado pelo SNS para filtrar a entrega.
create_queue_with_dlq "pedido" "pedido-service-queue" '{"eventType":["EstoqueInsuficiente"]}'
create_queue_with_dlq "estoque" "estoque-service-queue" '{"eventType":["PedidoCriado"]}'
create_queue_with_dlq "fiscal" "fiscal-service-queue" '{"eventType":["PedidoCriado","EstoqueInsuficiente"]}'
create_queue_with_dlq "notificacao" "notificacao-service-queue" '{"eventType":["PedidoCriado","NotaEmitida","NotaCancelada","SeparacaoPedidoIniciado"]}'
create_queue_with_dlq "expedicao" "expedicao-service-queue" '{"eventType":["NotaEmitida"]}'
create_queue_with_dlq "supplier" "supplier-service-queue" '{"eventType":["EstoqueAtualizado"]}'

cat <<EOF
SAGA_TOPIC_ARN=$topic_arn
AWS_ENDPOINT=$AWS_ENDPOINT
EOF
