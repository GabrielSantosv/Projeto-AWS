package com.marketflow.inventory.event.listener;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketflow.inventory.domain.ProcessedEvent;
import com.marketflow.inventory.event.EventEnvelope;
import com.marketflow.inventory.event.EventType;
import com.marketflow.inventory.event.dto.BillingRejectedPayload;
import com.marketflow.inventory.event.dto.EmployeeValidatedPayload;
import com.marketflow.inventory.event.dto.InvoiceIssuedPayload;
import com.marketflow.inventory.event.dto.InvoiceRejectedPayload;
import com.marketflow.inventory.repository.ProcessedEventRepository;
import com.marketflow.inventory.service.InventoryService;
import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.DeleteMessageRequest;
import software.amazon.awssdk.services.sqs.model.Message;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest;

@Component
public class SagaEventListener {

    private static final Logger log = LoggerFactory.getLogger(SagaEventListener.class);

    private final SqsClient sqsClient;
    private final ObjectMapper objectMapper;
    private final InventoryService inventoryService;
    private final ProcessedEventRepository processedEventRepository;

    @Value("${aws.sqs.inventory-queue-url}")
    private String inventoryQueueUrl;

    public SagaEventListener(
            SqsClient sqsClient,
            ObjectMapper objectMapper,
            InventoryService inventoryService,
            ProcessedEventRepository processedEventRepository
    ) {
        this.sqsClient = sqsClient;
        this.objectMapper = objectMapper;
        this.inventoryService = inventoryService;
        this.processedEventRepository = processedEventRepository;
    }

    @Scheduled(fixedDelayString = "${aws.sqs.poll-interval-ms}")
    public void poll() {
        sqsClient.receiveMessage(ReceiveMessageRequest.builder()
                        .queueUrl(inventoryQueueUrl)
                        .maxNumberOfMessages(10)
                        .waitTimeSeconds(5)
                        .build())
                .messages()
                .forEach(this::processAndDeleteOnSuccess);
    }

    private void processAndDeleteOnSuccess(Message message) {
        try {
            if (processMessage(message.body())) {
                sqsClient.deleteMessage(DeleteMessageRequest.builder()
                        .queueUrl(inventoryQueueUrl)
                        .receiptHandle(message.receiptHandle())
                        .build());
            }
        } catch (Exception exception) {
            log.warn("Inventory event processing failed. Message will remain available for retry.", exception);
        }
    }

    @Transactional
    public boolean processMessage(String body) throws IOException {
        JsonNode eventNode = unwrapSnsMessage(body);
        EventEnvelope<JsonNode> event = new EventEnvelope<>(
                eventNode.get("eventId").asText(),
                eventNode.get("eventType").asText(),
                eventNode.get("sagaId").asText(),
                eventNode.get("correlationId").asText(),
                java.time.Instant.parse(eventNode.get("timestamp").asText()),
                eventNode.get("version").asInt(),
                eventNode.get("payload")
        );

        if (processedEventRepository.existsById(event.eventId())) {
            return true;
        }

        route(event);
        processedEventRepository.save(new ProcessedEvent(event.eventId(), event.eventType()));
        return true;
    }

    private void route(EventEnvelope<JsonNode> event) {
        switch (event.eventType()) {
            case EventType.EMPLOYEE_VALIDATED -> {
                EmployeeValidatedPayload payload = objectMapper.convertValue(event.payload(), EmployeeValidatedPayload.class);
                inventoryService.reserveStock(payload.orderId(), event.correlationId(), payload.items());
            }
            case EventType.INVOICE_ISSUED -> {
                InvoiceIssuedPayload payload = objectMapper.convertValue(event.payload(), InvoiceIssuedPayload.class);
                inventoryService.confirmStock(payload.orderId(), event.correlationId());
            }
            case EventType.INVOICE_REJECTED -> {
                InvoiceRejectedPayload payload = objectMapper.convertValue(event.payload(), InvoiceRejectedPayload.class);
                inventoryService.releaseReservation(payload.orderId(), event.correlationId(), payload.reason());
            }
            case EventType.BILLING_REJECTED -> {
                BillingRejectedPayload payload = objectMapper.convertValue(event.payload(), BillingRejectedPayload.class);
                inventoryService.releaseReservation(payload.orderId(), event.correlationId(), payload.reason());
            }
            default -> log.info("Ignoring unsupported inventory event type: {}", event.eventType());
        }
    }

    private JsonNode unwrapSnsMessage(String body) throws IOException {
        JsonNode root = objectMapper.readTree(body);
        if (root.has("Message")) {
            return objectMapper.readTree(root.get("Message").asText());
        }
        return root;
    }
}
