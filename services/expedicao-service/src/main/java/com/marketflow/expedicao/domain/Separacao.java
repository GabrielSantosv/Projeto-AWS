package com.marketflow.expedicao.domain;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "separacoes")
public class Separacao {

    @Id
    private UUID id;

    @Column(name = "pedido_id", nullable = false, unique = true, length = 120)
    private String pedidoId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private StatusSeparacao status;

    @Column(name = "iniciada_em", nullable = false)
    private Instant iniciadaEm;

    @Column(name = "concluida_em")
    private Instant concluidaEm;

    protected Separacao() {
    }

    public Separacao(String pedidoId) {
        this.id = UUID.randomUUID();
        this.pedidoId = pedidoId;
        this.status = StatusSeparacao.INICIADA;
        this.iniciadaEm = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public String getPedidoId() {
        return pedidoId;
    }

    public StatusSeparacao getStatus() {
        return status;
    }

    public Instant getIniciadaEm() {
        return iniciadaEm;
    }

    public Instant getConcluidaEm() {
        return concluidaEm;
    }

    // TODO: concluir a separacao quando o fluxo futuro de expedicao for definido.
    public void concluir() {
        this.status = StatusSeparacao.CONCLUIDA;
        this.concluidaEm = Instant.now();
    }
}
