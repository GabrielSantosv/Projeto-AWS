package com.marketflow.estoque.controller;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import java.math.BigDecimal;

public final class ProdutoDtos {

    private ProdutoDtos() {
    }

    public record CriarProdutoRequest(
            @NotBlank String produtoId,
            @NotBlank String nome,
            @NotNull @DecimalMin("0.00") BigDecimal precoUnitario,
            @PositiveOrZero int quantidadeDisponivelTotal,
            @PositiveOrZero int limiteEstoqueBaixo
    ) {
    }

    public record AjustarEstoqueRequest(@NotNull Integer delta) {
    }

    public record ProdutoResponse(
            String produtoId,
            String nome,
            BigDecimal precoUnitario,
            int quantidadeDisponivelTotal,
            int quantidadeReservada,
            int quantidadeDisponivel,
            int limiteEstoqueBaixo,
            boolean estoqueBaixo
    ) {
    }
}
