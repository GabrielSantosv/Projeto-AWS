package com.marketflow.pedido.event.dto;

import java.util.List;

public record EstoqueInsuficientePayload(
        String pedidoId,
        String motivo,
        List<ItemPedidoDto> itens,
        List<ItemPedidoDto> itensIndisponiveis
) {

    public EstoqueInsuficientePayload {
        itens = itens == null ? List.of() : List.copyOf(itens);
        itensIndisponiveis = itensIndisponiveis == null ? List.of() : List.copyOf(itensIndisponiveis);
    }
}
