package com.marketflow.supplier.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "fornecedores")
public class Fornecedor {

    @Id
    private UUID id;

    @Column(name = "produto_id", nullable = false, length = 120)
    private String produtoId;

    @Column(nullable = false, length = 200)
    private String nome;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal preco;

    @Column(name = "prazo_dias", nullable = false)
    private int prazoDias;

    @Column(nullable = false)
    private boolean ativo;

    @Column(name = "criado_em", nullable = false)
    private Instant criadoEm;

    @Column(name = "atualizado_em", nullable = false)
    private Instant atualizadoEm;

    protected Fornecedor() {
    }

    public Fornecedor(String produtoId, String nome, BigDecimal preco, int prazoDias, boolean ativo) {
        if (produtoId == null || produtoId.isBlank()) {
            throw new IllegalArgumentException("produtoId nao pode ser vazio");
        }
        if (nome == null || nome.isBlank()) {
            throw new IllegalArgumentException("nome nao pode ser vazio");
        }
        if (preco == null || preco.signum() < 0) {
            throw new IllegalArgumentException("preco nao pode ser negativo");
        }
        if (prazoDias < 0) {
            throw new IllegalArgumentException("prazoDias nao pode ser negativo");
        }
        this.id = UUID.randomUUID();
        this.produtoId = produtoId;
        this.nome = nome;
        this.preco = preco;
        this.prazoDias = prazoDias;
        this.ativo = ativo;
        this.criadoEm = Instant.now();
        this.atualizadoEm = this.criadoEm;
    }

    public UUID getId() {
        return id;
    }

    public String getProdutoId() {
        return produtoId;
    }

    public String getNome() {
        return nome;
    }

    public BigDecimal getPreco() {
        return preco;
    }

    public int getPrazoDias() {
        return prazoDias;
    }

    public boolean isAtivo() {
        return ativo;
    }

    public Instant getCriadoEm() {
        return criadoEm;
    }

    public Instant getAtualizadoEm() {
        return atualizadoEm;
    }

    public void atualizar(String nome, BigDecimal preco, int prazoDias, boolean ativo) {
        if (nome == null || nome.isBlank() || preco == null || preco.signum() < 0 || prazoDias < 0) {
            throw new IllegalArgumentException("Dados de fornecedor invalidos");
        }
        this.nome = nome;
        this.preco = preco;
        this.prazoDias = prazoDias;
        this.ativo = ativo;
        this.atualizadoEm = Instant.now();
    }
}
