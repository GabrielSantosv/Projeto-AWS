package com.marketflow.estoque.event.publisher;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketflow.estoque.domain.EventoOutbox;
import com.marketflow.estoque.domain.StatusOutbox;
import com.marketflow.estoque.event.EnvelopeEvento;
import com.marketflow.estoque.repository.EventoOutboxRepository;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sns.model.MessageAttributeValue;
import software.amazon.awssdk.services.sns.model.PublishRequest;

@Component
public class PublicadorEventos {

    private final SnsClient snsClient;
    private final ObjectMapper objectMapper;
    private final EventoOutboxRepository outboxRepository;

    @Value("${aws.sns.saga-topic-arn}")
    private String sagaTopicArn;

    @Value("${aws.sns.outbox-batch-size}")
    private int outboxBatchSize;

    public PublicadorEventos(
            SnsClient snsClient,
            ObjectMapper objectMapper,
            EventoOutboxRepository outboxRepository
    ) {
        this.snsClient = snsClient;
        this.objectMapper = objectMapper;
        this.outboxRepository = outboxRepository;
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void publicar(EnvelopeEvento<?> evento) {
        try {
            outboxRepository.save(new EventoOutbox(evento, objectMapper.writeValueAsString(evento)));
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Nao foi possivel serializar evento " + evento.eventType(), exception);
        }
    }

    @Scheduled(fixedDelayString = "${aws.sns.outbox-poll-interval-ms}")
    @Transactional
    public void publicarPendentes() {
        outboxRepository.findByStatusForUpdate(StatusOutbox.PENDENTE, PageRequest.of(0, outboxBatchSize))
                .forEach(this::publicarNoBroker);
    }

    private void publicarNoBroker(EventoOutbox evento) {
        try {
            snsClient.publish(PublishRequest.builder()
                    .topicArn(sagaTopicArn)
                    .message(evento.getPayload())
                    .messageAttributes(Map.of(
                            "eventType",
                            MessageAttributeValue.builder()
                                    .dataType("String")
                                    .stringValue(evento.getEventType())
                                    .build()
                    ))
                    .build());
            evento.marcarPublicado();
        } catch (RuntimeException exception) {
            evento.registrarFalha(exception.getMessage());
        }
    }
}
