package com.marketflow.fiscal.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "notas_fiscais")
public class NotaFiscal {

    @Id
    private UUID id;

    @Column(name = "pedido_id", nullable = false, unique = true)
    private String pedidoId;

    @Column(name = "numero_nota", nullable = false, unique = true)
    private String numeroNota;

    @Column(name = "valor_total", nullable = false, precision = 12, scale = 2)
    private BigDecimal valorTotal;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private StatusNotaFiscal status;

    @Column(name = "emitida_em", nullable = false)
    private Instant emitidaEm;

    protected NotaFiscal() {
    }

    public NotaFiscal(String pedidoId, String numeroNota, BigDecimal valorTotal) {
        this.id = UUID.randomUUID();
        this.pedidoId = pedidoId;
        this.numeroNota = numeroNota;
        this.valorTotal = valorTotal;
        this.status = StatusNotaFiscal.EMITIDA;
        this.emitidaEm = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public String getPedidoId() {
        return pedidoId;
    }

    public String getNumeroNota() {
        return numeroNota;
    }

    public BigDecimal getValorTotal() {
        return valorTotal;
    }

    public StatusNotaFiscal getStatus() {
        return status;
    }

    public Instant getEmitidaEm() {
        return emitidaEm;
    }

    public void cancelar() {
        this.status = StatusNotaFiscal.CANCELADA;
    }
}
