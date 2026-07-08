package com.marketflow.inventory.event;

import java.time.Instant;
import java.util.UUID;

public record EventEnvelope<T>(
        String eventId,
        String eventType,
        String sagaId,
        String correlationId,
        Instant timestamp,
        int version,
        T payload
) {

    public static <T> EventEnvelope<T> create(String eventType, String sagaId, String correlationId, T payload) {
        return new EventEnvelope<>(
                UUID.randomUUID().toString(),
                eventType,
                sagaId,
                correlationId,
                Instant.now(),
                1,
                payload
        );
    }
}
