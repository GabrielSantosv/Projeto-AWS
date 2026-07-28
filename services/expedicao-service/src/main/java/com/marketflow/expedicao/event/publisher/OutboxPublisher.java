package com.marketflow.expedicao.event.publisher;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.marketflow.expedicao.domain.OutboxEvent;
import com.marketflow.expedicao.repository.OutboxEventRepository;

import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sns.model.MessageAttributeValue;
import software.amazon.awssdk.services.sns.model.PublishRequest;

@Component
public class OutboxPublisher {

    private static final Logger log = LoggerFactory.getLogger(OutboxPublisher.class);

    private final OutboxEventRepository outboxEventRepository;
    private final PublicadorBroker publicadorBroker;

    @Value("${outbox.batch-size:10}")
    private int batchSize;

    @Value("${outbox.polling-enabled:true}")
    private boolean pollingEnabled;

    public OutboxPublisher(OutboxEventRepository outboxEventRepository, PublicadorBroker publicadorBroker) {
        this.outboxEventRepository = outboxEventRepository;
        this.publicadorBroker = publicadorBroker;
    }

    @Scheduled(fixedDelayString = "${outbox.poll-interval-ms}")
    @Transactional
    public void publicarPendentes() {
        if (!pollingEnabled) {
            return;
        }
        publicarPendentesAgora();
    }

    @Transactional
    public void publicarPendentesAgora() {
        outboxEventRepository.findPendentesForUpdate(PageRequest.of(0, batchSize))
                .forEach(this::publicarNoBroker);
    }

    private void publicarNoBroker(OutboxEvent evento) {
        try {
            publicadorBroker.publicar(evento);
            evento.marcarPublicado();
        } catch (RuntimeException exception) {
            log.warn("Falha ao publicar evento {} no SNS; permanecera no outbox.", evento.getEventId(), exception);
        }
    }

    @Component
    static class PublicadorBroker {

        private final SnsClient snsClient;

        @Value("${aws.sns.saga-topic-arn}")
        private String sagaTopicArn;

        PublicadorBroker(SnsClient snsClient) {
            this.snsClient = snsClient;
        }

        void publicar(OutboxEvent evento) {
            snsClient.publish(PublishRequest.builder()
                    .topicArn(sagaTopicArn)
                    .message(evento.getPayloadJson())
                    .messageAttributes(Map.of(
                            "eventType",
                            MessageAttributeValue.builder()
                                    .dataType("String")
                                    .stringValue(evento.getEventType())
                                    .build()
                    ))
                    .build());
        }
    }
}
