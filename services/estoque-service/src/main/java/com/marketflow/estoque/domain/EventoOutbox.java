package com.marketflow.estoque.domain;

import com.marketflow.estoque.event.EnvelopeEvento;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "outbox_events")
public class EventoOutbox {

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

    @Column(nullable = false, columnDefinition = "text")
    private String payload;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private StatusOutbox status;

    @Column(nullable = false)
    private int tentativas;

    @Column(name = "ultimo_erro", columnDefinition = "text")
    private String ultimoErro;

    @Column(name = "criado_em", nullable = false)
    private Instant criadoEm;

    @Column(name = "publicado_em")
    private Instant publicadoEm;

    protected EventoOutbox() {
    }

    public EventoOutbox(EnvelopeEvento<?> evento, String payload) {
        this.id = UUID.randomUUID();
        this.eventId = evento.eventId();
        this.eventType = evento.eventType();
        this.sagaId = evento.sagaId();
        this.correlationId = evento.correlationId();
        this.payload = payload;
        this.status = StatusOutbox.PENDENTE;
        this.tentativas = 0;
        this.criadoEm = Instant.now();
    }

    public String getEventType() {
        return eventType;
    }

    public String getPayload() {
        return payload;
    }

    public void marcarPublicado() {
        status = StatusOutbox.PUBLICADO;
        publicadoEm = Instant.now();
        ultimoErro = null;
    }

    public void registrarFalha(String erro) {
        tentativas++;
        ultimoErro = erro;
    }
}
