package com.marketflow.inventory.event.publisher;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketflow.inventory.event.EventEnvelope;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sns.model.MessageAttributeValue;
import software.amazon.awssdk.services.sns.model.PublishRequest;

@Component
public class EventPublisher {

    private final SnsClient snsClient;
    private final ObjectMapper objectMapper;

    @Value("${aws.sns.saga-topic-arn}")
    private String sagaTopicArn;

    public EventPublisher(SnsClient snsClient, ObjectMapper objectMapper) {
        this.snsClient = snsClient;
        this.objectMapper = objectMapper;
    }

    public void publish(EventEnvelope<?> event) {
        try {
            snsClient.publish(PublishRequest.builder()
                    .topicArn(sagaTopicArn)
                    .message(objectMapper.writeValueAsString(event))
                    .messageAttributes(java.util.Map.of(
                            "eventType",
                            MessageAttributeValue.builder()
                                    .dataType("String")
                                    .stringValue(event.eventType())
                                    .build()
                    ))
                    .build());
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Could not serialize event " + event.eventType(), exception);
        }
    }
}
