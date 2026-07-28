package com.marketflow.supplier.controller;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import com.marketflow.supplier.domain.Fornecedor;

public final class FornecedorDtos {

    private FornecedorDtos() {
    }

    public record CadastroRequest(
            @NotBlank String produtoId,
            @NotBlank String nome,
            @NotNull @PositiveOrZero BigDecimal preco,
            @PositiveOrZero int prazoDias,
            boolean ativo
    ) {
    }

    public record Resposta(
            UUID id,
            String produtoId,
            String nome,
            BigDecimal preco,
            int prazoDias,
            boolean ativo,
            Instant criadoEm,
            Instant atualizadoEm
    ) {
        public static Resposta from(Fornecedor fornecedor) {
            return new Resposta(
                    fornecedor.getId(),
                    fornecedor.getProdutoId(),
                    fornecedor.getNome(),
                    fornecedor.getPreco(),
                    fornecedor.getPrazoDias(),
                    fornecedor.isAtivo(),
                    fornecedor.getCriadoEm(),
                    fornecedor.getAtualizadoEm()
            );
        }
    }
}
