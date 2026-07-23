package com.marketflow.estoque.event.listener;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketflow.estoque.event.EnvelopeEvento;
import com.marketflow.estoque.event.TipoEvento;
import com.marketflow.estoque.event.dto.PedidoCriadoPayload;
import com.marketflow.estoque.service.EstoqueService;
import java.io.IOException;
import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.DeleteMessageRequest;
import software.amazon.awssdk.services.sqs.model.Message;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest;

@Component
public class OuvinteEventosSaga {

    private static final Logger log = LoggerFactory.getLogger(OuvinteEventosSaga.class);

    private final SqsClient sqsClient;
    private final ObjectMapper objectMapper;
    private final EstoqueService estoqueService;

    @Value("${aws.sqs.estoque-queue-url}")
    private String estoqueQueueUrl;

    public OuvinteEventosSaga(
            SqsClient sqsClient,
            ObjectMapper objectMapper,
            EstoqueService estoqueService
    ) {
        this.sqsClient = sqsClient;
        this.objectMapper = objectMapper;
        this.estoqueService = estoqueService;
    }

    @Scheduled(fixedDelayString = "${aws.sqs.poll-interval-ms}")
    public void poll() {
        sqsClient.receiveMessage(ReceiveMessageRequest.builder()
                        .queueUrl(estoqueQueueUrl)
                        .maxNumberOfMessages(10)
                        .waitTimeSeconds(5)
                        .build())
                .messages()
                .forEach(this::processarEExcluirAoConcluir);
    }

    private void processarEExcluirAoConcluir(Message message) {
        try {
            if (processarMensagem(message.body())) {
                sqsClient.deleteMessage(DeleteMessageRequest.builder()
                        .queueUrl(estoqueQueueUrl)
                        .receiptHandle(message.receiptHandle())
                        .build());
            }
        } catch (Exception exception) {
            log.warn("Falha ao processar evento de estoque. Mensagem ficara disponivel para retry.", exception);
        }
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

        if (!TipoEvento.PEDIDO_CRIADO.equals(evento.eventType())) {
            log.info("Evento ignorado pelo estoque-service: {}", evento.eventType());
            return true;
        }

        PedidoCriadoPayload payload = objectMapper.convertValue(evento.payload(), PedidoCriadoPayload.class);
        estoqueService.processarPedidoCriado(evento.withPayload(payload));
        return true;
    }

    private JsonNode unwrapSnsMessage(String body) throws IOException {
        JsonNode root = objectMapper.readTree(body);
        if (root.has("Message")) {
            return objectMapper.readTree(root.get("Message").asText());
        }
        return root;
    }
}
