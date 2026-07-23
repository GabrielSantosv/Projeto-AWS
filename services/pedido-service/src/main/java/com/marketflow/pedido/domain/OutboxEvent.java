package com.marketflow.pedido.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "outbox_events")
public class OutboxEvent {

    @Id
    private UUID id;

    @Column(name = "evento_id", nullable = false, unique = true, length = 80)
    private String eventoId;

    @Column(name = "tipo_evento", nullable = false, length = 120)
    private String tipoEvento;

    @Column(name = "saga_id", nullable = false, length = 120)
    private String sagaId;

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

    public OutboxEvent(String eventoId, String tipoEvento, String sagaId, String payloadJson) {
        this.id = UUID.randomUUID();
        this.eventoId = eventoId;
        this.tipoEvento = tipoEvento;
        this.sagaId = sagaId;
        this.payloadJson = payloadJson;
        this.publicado = false;
        this.criadoEm = Instant.now();
    }

    public String getEventoId() {
        return eventoId;
    }

    public String getTipoEvento() {
        return tipoEvento;
    }

    public String getSagaId() {
        return sagaId;
    }

    public String getPayloadJson() {
        return payloadJson;
    }

    public boolean isPublicado() {
        return publicado;
    }

    public void marcarPublicado() {
        publicado = true;
        publicadoEm = Instant.now();
    }
}
