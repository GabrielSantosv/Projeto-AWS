package com.marketflow.estoque.event.dto;

import java.util.List;

public record EstoqueAtualizadoPayload(
        String pedidoId,
        List<ItemEstoqueAtualizado> itens,
        boolean estoqueBaixo
) {

    public EstoqueAtualizadoPayload {
        itens = itens == null ? List.of() : List.copyOf(itens);
    }

    public record ItemEstoqueAtualizado(
            String produtoId,
            int quantidadeReservada,
            int quantidadeDisponivel,
            int limiteEstoqueBaixo,
            boolean estoqueBaixo
    ) {
    }
}
