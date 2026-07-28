package com.marketflow.pedido.event.publisher;

import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.marketflow.pedido.domain.OutboxEvent;

import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sns.model.MessageAttributeValue;
import software.amazon.awssdk.services.sns.model.PublishRequest;

@Component
public class PublicadorBroker {

    private final SnsClient snsClient;

    @Value("${aws.sns.saga-topic-arn}")
    private String sagaTopicArn;

    public PublicadorBroker(SnsClient snsClient) {
        this.snsClient = snsClient;
    }

    public void publicar(OutboxEvent evento) {
        snsClient.publish(PublishRequest.builder()
                .topicArn(sagaTopicArn)
                .message(evento.getPayloadJson())
                .messageAttributes(Map.of(
                        "eventType",
                        MessageAttributeValue.builder()
                                .dataType("String")
                                .stringValue(evento.getTipoEvento())
                                .build()
                ))
                .build());
    }
}
