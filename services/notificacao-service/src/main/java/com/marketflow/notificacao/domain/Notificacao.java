package com.marketflow.notificacao.domain;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "notificacoes")
public class Notificacao {

    @Id
    private UUID id;

    @Column(name = "pedido_id", nullable = false, length = 120)
    private String pedidoId;

    @Column(name = "tipo_evento", nullable = false, length = 120)
    private String tipoEvento;

    @Column(nullable = false, length = 500)
    private String mensagem;

    @Column(nullable = false, length = 40)
    private String canal;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private StatusNotificacao status;

    @Column(name = "enviada_em", nullable = false)
    private Instant enviadaEm;

    protected Notificacao() {
    }

    public Notificacao(String pedidoId, String tipoEvento, String mensagem, String canal, StatusNotificacao status) {
        this.id = UUID.randomUUID();
        this.pedidoId = pedidoId;
        this.tipoEvento = tipoEvento;
        this.mensagem = mensagem;
        this.canal = canal;
        this.status = status;
        this.enviadaEm = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public String getPedidoId() {
        return pedidoId;
    }

    public String getTipoEvento() {
        return tipoEvento;
    }

    public String getMensagem() {
        return mensagem;
    }

    public String getCanal() {
        return canal;
    }

    public StatusNotificacao getStatus() {
        return status;
    }

    public Instant getEnviadaEm() {
        return enviadaEm;
    }
}
