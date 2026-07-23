package com.marketflow.estoque.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
        name = "reservas_estoque",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_reservas_estoque_pedido_produto",
                columnNames = {"pedido_id", "produto_id"}
        )
)
public class ReservaEstoque {

    @Id
    private UUID id;

    @Column(name = "pedido_id", nullable = false, length = 120)
    private String pedidoId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "produto_id", nullable = false)
    private Produto produto;

    @Column(nullable = false)
    private int quantidade;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private StatusReserva status;

    @Column(name = "criado_em", nullable = false)
    private Instant criadoEm;

    @Column(name = "atualizado_em", nullable = false)
    private Instant atualizadoEm;

    protected ReservaEstoque() {
    }

    public ReservaEstoque(String pedidoId, Produto produto, int quantidade) {
        this.id = UUID.randomUUID();
        this.pedidoId = pedidoId;
        this.produto = produto;
        this.quantidade = quantidade;
        this.status = StatusReserva.RESERVADA;
        this.criadoEm = Instant.now();
        this.atualizadoEm = this.criadoEm;
    }

    public String getPedidoId() {
        return pedidoId;
    }

    public Produto getProduto() {
        return produto;
    }

    public int getQuantidade() {
        return quantidade;
    }

    public StatusReserva getStatus() {
        return status;
    }

    public void cancelar() {
        status = StatusReserva.CANCELADA;
        atualizadoEm = Instant.now();
    }
}
