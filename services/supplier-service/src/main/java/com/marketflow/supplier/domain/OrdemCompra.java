package com.marketflow.supplier.domain;

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
@Table(name = "ordens_compra")
public class OrdemCompra {

    @Id
    private UUID id;

    @Column(name = "produto_id", nullable = false, length = 120)
    private String produtoId;

    @Column(name = "fornecedor_id", nullable = false)
    private UUID fornecedorId;

    @Column(nullable = false)
    private int quantidade;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal preco;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private StatusOrdemCompra status;

    @Column(name = "criada_em", nullable = false)
    private Instant criadaEm;

    @Column(name = "confirmada_em")
    private Instant confirmadaEm;

    protected OrdemCompra() {
    }

    public OrdemCompra(String produtoId, UUID fornecedorId, int quantidade, BigDecimal preco) {
        if (quantidade <= 0) {
            throw new IllegalArgumentException("quantidade deve ser maior que zero");
        }
        this.id = UUID.randomUUID();
        this.produtoId = produtoId;
        this.fornecedorId = fornecedorId;
        this.quantidade = quantidade;
        this.preco = preco;
        this.status = StatusOrdemCompra.GERADA;
        this.criadaEm = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public String getProdutoId() {
        return produtoId;
    }

    public UUID getFornecedorId() {
        return fornecedorId;
    }

    public int getQuantidade() {
        return quantidade;
    }

    public BigDecimal getPreco() {
        return preco;
    }

    public StatusOrdemCompra getStatus() {
        return status;
    }

    public Instant getCriadaEm() {
        return criadaEm;
    }

    public Instant getConfirmadaEm() {
        return confirmadaEm;
    }

    public void confirmar() {
        this.status = StatusOrdemCompra.CONFIRMADA;
        this.confirmadaEm = Instant.now();
    }
}
