package com.marketflow.estoque.event.dto;

import java.util.List;

public record EstoqueInsuficientePayload(
        String pedidoId,
        String motivo,
        List<ItemPedido> itens,
        List<ItemPedido> itensIndisponiveis
) {

    public EstoqueInsuficientePayload {
        itens = itens == null ? List.of() : List.copyOf(itens);
        itensIndisponiveis = itensIndisponiveis == null ? List.of() : List.copyOf(itensIndisponiveis);
    }
}
