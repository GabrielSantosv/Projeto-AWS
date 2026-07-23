package com.marketflow.fiscal.domain;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "pedidos_cancelados")
public class PedidoCancelado {

    @Id
    private UUID id;

    @Column(name = "pedido_id", nullable = false, unique = true)
    private String pedidoId;

    @Column(nullable = false, length = 120)
    private String motivo;

    @Column(name = "cancelado_em", nullable = false)
    private Instant canceladoEm;

    protected PedidoCancelado() {
    }

    public PedidoCancelado(String pedidoId, String motivo) {
        this.id = UUID.randomUUID();
        this.pedidoId = pedidoId;
        this.motivo = motivo;
        this.canceladoEm = Instant.now();
    }

    public String getPedidoId() {
        return pedidoId;
    }

    public String getMotivo() {
        return motivo;
    }
}
