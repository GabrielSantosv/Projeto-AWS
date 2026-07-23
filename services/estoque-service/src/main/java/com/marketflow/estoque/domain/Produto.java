package com.marketflow.estoque.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "produtos")
public class Produto {

    @Id
    @Column(name = "produto_id", nullable = false, length = 120)
    private String produtoId;

    @Column(nullable = false, length = 255)
    private String nome;

    @Column(name = "preco_unitario", nullable = false, precision = 12, scale = 2)
    private BigDecimal precoUnitario;

    @Column(name = "quantidade_disponivel_total", nullable = false)
    private int quantidadeDisponivelTotal;

    @Column(name = "quantidade_reservada", nullable = false)
    private int quantidadeReservada;

    @Column(name = "limite_estoque_baixo", nullable = false)
    private int limiteEstoqueBaixo;

    @Column(name = "criado_em", nullable = false)
    private Instant criadoEm;

    @Column(name = "atualizado_em", nullable = false)
    private Instant atualizadoEm;

    protected Produto() {
    }

    public Produto(
            String produtoId,
            String nome,
            BigDecimal precoUnitario,
            int quantidadeDisponivelTotal,
            int limiteEstoqueBaixo
    ) {
        if (produtoId == null || produtoId.isBlank()) {
            throw new IllegalArgumentException("produtoId nao pode ser vazio");
        }
        if (quantidadeDisponivelTotal < 0) {
            throw new IllegalArgumentException("quantidadeDisponivelTotal nao pode ser negativa");
        }
        if (limiteEstoqueBaixo < 0) {
            throw new IllegalArgumentException("limiteEstoqueBaixo nao pode ser negativo");
        }
        this.produtoId = produtoId;
        this.nome = nome;
        this.precoUnitario = precoUnitario;
        this.quantidadeDisponivelTotal = quantidadeDisponivelTotal;
        this.quantidadeReservada = 0;
        this.limiteEstoqueBaixo = limiteEstoqueBaixo;
        this.criadoEm = Instant.now();
        this.atualizadoEm = this.criadoEm;
    }

    public String getProdutoId() {
        return produtoId;
    }

    public String getNome() {
        return nome;
    }

    public BigDecimal getPrecoUnitario() {
        return precoUnitario;
    }

    public int getQuantidadeDisponivelTotal() {
        return quantidadeDisponivelTotal;
    }

    public int getQuantidadeReservada() {
        return quantidadeReservada;
    }

    public int getLimiteEstoqueBaixo() {
        return limiteEstoqueBaixo;
    }

    public int getQuantidadeDisponivel() {
        return quantidadeDisponivelTotal - quantidadeReservada;
    }

    public boolean temEstoqueSuficiente(int quantidade) {
        return quantidade > 0 && getQuantidadeDisponivel() >= quantidade;
    }

    public boolean isEstoqueBaixo() {
        return getQuantidadeDisponivel() <= limiteEstoqueBaixo;
    }

    public void reservar(int quantidade) {
        if (!temEstoqueSuficiente(quantidade)) {
            throw new IllegalStateException("Estoque insuficiente para reservar");
        }
        quantidadeReservada += quantidade;
        tocar();
    }

    public void liberarReserva(int quantidade) {
        if (quantidade <= 0) {
            throw new IllegalArgumentException("Quantidade deve ser maior que zero");
        }
        if (quantidadeReservada < quantidade) {
            throw new IllegalStateException("Reserva nao pode ficar negativa");
        }
        quantidadeReservada -= quantidade;
        tocar();
    }

    public void ajustarEstoque(int delta) {
        if (delta == 0) {
            throw new IllegalArgumentException("Ajuste de estoque nao pode ser zero");
        }
        int proximaQuantidade = quantidadeDisponivelTotal + delta;
        if (proximaQuantidade < quantidadeReservada) {
            throw new IllegalArgumentException("Ajuste deixaria estoque menor que a quantidade reservada");
        }
        quantidadeDisponivelTotal = proximaQuantidade;
        tocar();
    }

    private void tocar() {
        atualizadoEm = Instant.now();
    }
}
