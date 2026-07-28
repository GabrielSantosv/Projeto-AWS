package com.marketflow.supplier.event.listener;

import java.io.IOException;
import java.time.Instant;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketflow.supplier.event.EnvelopeEvento;
import com.marketflow.supplier.event.TipoEvento;
import com.marketflow.supplier.event.dto.EstoqueAtualizadoPayload;
import com.marketflow.supplier.service.SupplierService;

import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.DeleteMessageRequest;
import software.amazon.awssdk.services.sqs.model.Message;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest;

@Component
public class OuvinteEventosSaga {

    private static final Logger log = LoggerFactory.getLogger(OuvinteEventosSaga.class);

    private final SqsClient sqsClient;
    private final ObjectMapper objectMapper;
    private final SupplierService supplierService;

    @Value("${aws.sqs.supplier-queue-url}")
    private String supplierQueueUrl;

    @Value("${aws.sqs.polling-enabled:true}")
    private boolean pollingEnabled;

    public OuvinteEventosSaga(SqsClient sqsClient, ObjectMapper objectMapper, SupplierService supplierService) {
        this.sqsClient = sqsClient;
        this.objectMapper = objectMapper;
        this.supplierService = supplierService;
    }

    @Scheduled(fixedDelayString = "${aws.sqs.poll-interval-ms}")
    public void poll() {
        if (!pollingEnabled) {
            return;
        }
        sqsClient.receiveMessage(ReceiveMessageRequest.builder()
                        .queueUrl(supplierQueueUrl)
                        .maxNumberOfMessages(10)
                        .waitTimeSeconds(5)
                        .build())
                .messages()
                .forEach(this::processarEExcluirAoConcluir);
    }

    public boolean processarMensagem(String body) throws IOException {
        JsonNode eventNode = unwrapSnsMessage(body);
        EnvelopeEvento<JsonNode> evento = new EnvelopeEvento<>(
                eventNode.get("eventId").asText(),
                eventNode.get("eventType").asText(),
                eventNode.get("sagaId").asText(),
                eventNode.get("correlationId").asText(),
                Instant.parse(eventNode.get("timestamp").asText()),
                eventNode.get("version").asInt(),
                eventNode.get("payload")
        );

        if (!TipoEvento.ESTOQUE_ATUALIZADO.equals(evento.eventType())) {
            log.info("Evento ignorado pelo supplier-service: {}", evento.eventType());
            return true;
        }

        EstoqueAtualizadoPayload payload = objectMapper.convertValue(
                evento.payload(),
                EstoqueAtualizadoPayload.class
        );
        supplierService.processarEstoqueAtualizado(evento.withPayload(payload));
        return true;
    }

    private void processarEExcluirAoConcluir(Message message) {
        try {
            if (processarMensagem(message.body())) {
                sqsClient.deleteMessage(DeleteMessageRequest.builder()
                        .queueUrl(supplierQueueUrl)
                        .receiptHandle(message.receiptHandle())
                        .build());
            }
        } catch (Exception exception) {
            log.warn("Falha ao processar evento de supplier. Mensagem permanecera para retry.", exception);
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
