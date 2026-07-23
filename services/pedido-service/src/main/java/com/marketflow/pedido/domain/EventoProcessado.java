package com.marketflow.pedido.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "eventos_processados")
public class EventoProcessado {

    @Id
    @Column(name = "event_id", nullable = false, length = 80)
    private String eventId;

    @Column(name = "event_type", nullable = false, length = 120)
    private String eventType;

    @Column(name = "processado_em", nullable = false)
    private Instant processadoEm;

    protected EventoProcessado() {
    }

    public EventoProcessado(String eventId, String eventType) {
        this.eventId = eventId;
        this.eventType = eventType;
        this.processadoEm = Instant.now();
    }
}
