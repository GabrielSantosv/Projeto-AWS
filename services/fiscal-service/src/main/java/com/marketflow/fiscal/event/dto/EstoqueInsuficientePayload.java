package com.marketflow.fiscal.event.dto;

import java.util.List;

public record EstoqueInsuficientePayload(
        String pedidoId,
        String motivo,
        List<ItemPedidoDto> itensSemEstoque,
        List<ItemPedidoDto> itensPedido
) {
}
