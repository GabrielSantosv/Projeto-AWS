package com.marketflow.notificacao.event.listener;

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
import com.marketflow.notificacao.event.EnvelopeEvento;
import com.marketflow.notificacao.event.TipoEvento;
import com.marketflow.notificacao.event.dto.NotaCanceladaPayload;
import com.marketflow.notificacao.event.dto.NotaEmitidaPayload;
import com.marketflow.notificacao.event.dto.PedidoCriadoPayload;
import com.marketflow.notificacao.event.dto.SeparacaoPedidoIniciadoPayload;
import com.marketflow.notificacao.service.NotificacaoService;

import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.DeleteMessageRequest;
import software.amazon.awssdk.services.sqs.model.Message;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest;

@Component
public class OuvinteEventosSaga {

    private static final Logger log = LoggerFactory.getLogger(OuvinteEventosSaga.class);
    private static final Set<String> TIPOS_TRATADOS = Set.of(
            TipoEvento.PEDIDO_CRIADO,
            TipoEvento.NOTA_EMITIDA,
            TipoEvento.NOTA_CANCELADA,
            TipoEvento.SEPARACAO_PEDIDO_INICIADO
    );

    private final SqsClient sqsClient;
    private final ObjectMapper objectMapper;
    private final NotificacaoService notificacaoService;

    @Value("${aws.sqs.notificacao-queue-url}")
    private String notificacaoQueueUrl;

    @Value("${aws.sqs.polling-enabled:true}")
    private boolean pollingEnabled;

    public OuvinteEventosSaga(
            SqsClient sqsClient,
            ObjectMapper objectMapper,
            NotificacaoService notificacaoService
    ) {
        this.sqsClient = sqsClient;
        this.objectMapper = objectMapper;
        this.notificacaoService = notificacaoService;
    }

    @Scheduled(fixedDelayString = "${aws.sqs.poll-interval-ms}")
    public void poll() {
        if (!pollingEnabled) {
            return;
        }
        sqsClient.receiveMessage(ReceiveMessageRequest.builder()
                        .queueUrl(notificacaoQueueUrl)
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

        switch (evento.eventType()) {
            case TipoEvento.PEDIDO_CRIADO -> notificacaoService.processarPedidoCriado(evento.withPayload(
                    objectMapper.convertValue(evento.payload(), PedidoCriadoPayload.class)
            ));
            case TipoEvento.NOTA_EMITIDA -> notificacaoService.processarNotaEmitida(evento.withPayload(
                    objectMapper.convertValue(evento.payload(), NotaEmitidaPayload.class)
            ));
            case TipoEvento.NOTA_CANCELADA -> notificacaoService.processarNotaCancelada(evento.withPayload(
                    objectMapper.convertValue(evento.payload(), NotaCanceladaPayload.class)
            ));
            case TipoEvento.SEPARACAO_PEDIDO_INICIADO -> notificacaoService.processarSeparacaoPedidoIniciado(
                    evento.withPayload(objectMapper.convertValue(
                            evento.payload(),
                            SeparacaoPedidoIniciadoPayload.class
                    ))
            );
            default -> log.info("Evento ignorado pelo notificacao-service: {}", evento.eventType());
        }
        return true;
    }

    private void processarEExcluirAoConcluir(Message message) {
        try {
            if (processarMensagem(message.body())) {
                sqsClient.deleteMessage(DeleteMessageRequest.builder()
                        .queueUrl(notificacaoQueueUrl)
                        .receiptHandle(message.receiptHandle())
                        .build());
            }
        } catch (Exception exception) {
            log.warn("Falha ao processar notificacao. Mensagem permanecera disponivel para retry.", exception);
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
