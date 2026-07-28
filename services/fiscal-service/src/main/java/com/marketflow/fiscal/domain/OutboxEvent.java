package com.marketflow.fiscal.domain;

import java.time.Instant;
import java.util.UUID;

import com.marketflow.fiscal.event.EnvelopeEvento;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "outbox_events")
public class OutboxEvent {

    @Id
    private UUID id;

    @Column(name = "event_id", nullable = false, unique = true, length = 80)
    private String eventId;

    @Column(name = "event_type", nullable = false, length = 120)
    private String eventType;

    @Column(name = "saga_id", nullable = false, length = 120)
    private String sagaId;

    @Column(name = "correlation_id", nullable = false, length = 120)
    private String correlationId;

    @Column(name = "payload_json", nullable = false, columnDefinition = "text")
    private String payloadJson;

    @Column(nullable = false)
    private boolean publicado;

    @Column(name = "criado_em", nullable = false)
    private Instant criadoEm;

    @Column(name = "publicado_em")
    private Instant publicadoEm;

    protected OutboxEvent() {
    }

    public OutboxEvent(EnvelopeEvento<?> evento, String payloadJson) {
        this.id = UUID.randomUUID();
        this.eventId = evento.eventId();
        this.eventType = evento.eventType();
        this.sagaId = evento.sagaId();
        this.correlationId = evento.correlationId();
        this.payloadJson = payloadJson;
        this.publicado = false;
        this.criadoEm = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public String getEventId() {
        return eventId;
    }

    public String getEventType() {
        return eventType;
    }

    public String getPayloadJson() {
        return payloadJson;
    }

    public boolean isPublicado() {
        return publicado;
    }

    public Instant getCriadoEm() {
        return criadoEm;
    }

    public void marcarPublicado() {
        this.publicado = true;
        this.publicadoEm = Instant.now();
    }
}
