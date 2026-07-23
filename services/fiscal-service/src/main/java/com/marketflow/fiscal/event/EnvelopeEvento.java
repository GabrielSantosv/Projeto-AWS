package com.marketflow.fiscal.event;

import java.time.Instant;
import java.util.UUID;

public record EnvelopeEvento<T>(
        String eventId,
        String eventType,
        String sagaId,
        String correlationId,
        Instant timestamp,
        int version,
        T payload
) {

    public static <T> EnvelopeEvento<T> create(String eventType, String sagaId, String correlationId, T payload) {
        return new EnvelopeEvento<>(
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
