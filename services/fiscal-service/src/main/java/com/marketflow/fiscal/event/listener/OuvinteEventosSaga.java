package com.marketflow.fiscal.event.listener;

import java.io.IOException;
import java.time.Instant;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketflow.fiscal.event.EnvelopeEvento;
import com.marketflow.fiscal.event.TipoEvento;
import com.marketflow.fiscal.event.dto.EstoqueInsuficientePayload;
import com.marketflow.fiscal.event.dto.PedidoCriadoPayload;
import com.marketflow.fiscal.service.FiscalService;

import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.DeleteMessageRequest;
import software.amazon.awssdk.services.sqs.model.Message;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest;

@Component
public class OuvinteEventosSaga {

    private static final Logger log = LoggerFactory.getLogger(OuvinteEventosSaga.class);

    private final SqsClient sqsClient;
    private final ObjectMapper objectMapper;
    private final FiscalService fiscalService;

    @Value("${aws.sqs.fiscal-queue-url}")
    private String fiscalQueueUrl;

    @Value("${aws.sqs.polling-enabled:true}")
    private boolean pollingEnabled;

    public OuvinteEventosSaga(SqsClient sqsClient, ObjectMapper objectMapper, FiscalService fiscalService) {
        this.sqsClient = sqsClient;
        this.objectMapper = objectMapper;
        this.fiscalService = fiscalService;
    }

    @Scheduled(fixedDelayString = "${aws.sqs.poll-interval-ms}")
    public void poll() {
        if (!pollingEnabled) {
            return;
        }
        sqsClient.receiveMessage(ReceiveMessageRequest.builder()
                        .queueUrl(fiscalQueueUrl)
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

        switch (evento.eventType()) {
            case TipoEvento.PEDIDO_CRIADO -> {
                PedidoCriadoPayload payload = objectMapper.convertValue(evento.payload(), PedidoCriadoPayload.class);
                fiscalService.emitirNotaFiscal(evento.withPayload(payload));
            }
            case TipoEvento.ESTOQUE_INSUFICIENTE -> {
                EstoqueInsuficientePayload payload = objectMapper.convertValue(
                        evento.payload(), EstoqueInsuficientePayload.class);
                fiscalService.cancelarPorEstoqueInsuficiente(evento.withPayload(payload));
            }
            default -> log.info("Evento ignorado pelo fiscal-service: {}", evento.eventType());
        }
        return true;
    }

    private void processarEExcluirAoConcluir(Message message) {
        try {
            if (processarMensagem(message.body())) {
                sqsClient.deleteMessage(DeleteMessageRequest.builder()
                        .queueUrl(fiscalQueueUrl)
                        .receiptHandle(message.receiptHandle())
                        .build());
            }
        } catch (Exception exception) {
            log.warn("Falha ao processar evento fiscal. Mensagem permanecera para retry.", exception);
        }
    }

    private JsonNode unwrapSnsMessage(String body) throws IOException {
        JsonNode root = objectMapper.readTree(body);
        if (root.has("Message")) {
            return objectMapper.readTree(root.get("Message").asText());
        }
        return root;
    }

    public void consumirPedidoCriado(EnvelopeEvento<PedidoCriadoPayload> evento) {
        fiscalService.emitirNotaFiscal(evento);
    }

    public void consumirEstoqueInsuficiente(EnvelopeEvento<EstoqueInsuficientePayload> evento) {
        fiscalService.cancelarPorEstoqueInsuficiente(evento);
    }
}
