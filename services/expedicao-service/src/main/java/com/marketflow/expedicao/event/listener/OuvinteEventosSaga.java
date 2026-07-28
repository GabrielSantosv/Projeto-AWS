package com.marketflow.expedicao.event.listener;

import java.io.IOException;
import java.time.Instant;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketflow.expedicao.event.EnvelopeEvento;
import com.marketflow.expedicao.event.TipoEvento;
import com.marketflow.expedicao.event.dto.NotaEmitidaPayload;
import com.marketflow.expedicao.service.ExpedicaoService;

import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.DeleteMessageRequest;
import software.amazon.awssdk.services.sqs.model.Message;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest;

@Component
public class OuvinteEventosSaga {

    private static final Logger log = LoggerFactory.getLogger(OuvinteEventosSaga.class);
    private static final Set<String> TIPOS_TRATADOS = Set.of(TipoEvento.NOTA_EMITIDA);

    private final SqsClient sqsClient;
    private final ObjectMapper objectMapper;
    private final ExpedicaoService expedicaoService;

    @Value("${aws.sqs.expedicao-queue-url}")
    private String expedicaoQueueUrl;

    @Value("${aws.sqs.polling-enabled:true}")
    private boolean pollingEnabled;

    public OuvinteEventosSaga(
            SqsClient sqsClient,
            ObjectMapper objectMapper,
            ExpedicaoService expedicaoService
    ) {
        this.sqsClient = sqsClient;
        this.objectMapper = objectMapper;
        this.expedicaoService = expedicaoService;
    }

    @Scheduled(fixedDelayString = "${aws.sqs.poll-interval-ms}")
    public void poll() {
        if (!pollingEnabled) {
            return;
        }
        sqsClient.receiveMessage(ReceiveMessageRequest.builder()
                        .queueUrl(expedicaoQueueUrl)
                        .maxNumberOfMessages(10)
                        .waitTimeSeconds(5)
                        .build())
                .messages()
                .forEach(this::processarEExcluirAoConcluir);
    }

    public Set<String> tiposTratados() {
        return TIPOS_TRATADOS;
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

        if (!TipoEvento.NOTA_EMITIDA.equals(evento.eventType())) {
            log.info("Evento ignorado pelo expedicao-service: {}", evento.eventType());
            return true;
        }

        NotaEmitidaPayload payload = objectMapper.convertValue(evento.payload(), NotaEmitidaPayload.class);
        expedicaoService.iniciarSeparacao(evento.withPayload(payload));
        return true;
    }

    private void processarEExcluirAoConcluir(Message message) {
        try {
            if (processarMensagem(message.body())) {
                sqsClient.deleteMessage(DeleteMessageRequest.builder()
                        .queueUrl(expedicaoQueueUrl)
                        .receiptHandle(message.receiptHandle())
                        .build());
            }
        } catch (Exception exception) {
            log.warn("Falha ao processar evento de expedicao. Mensagem permanecera para retry.", exception);
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
