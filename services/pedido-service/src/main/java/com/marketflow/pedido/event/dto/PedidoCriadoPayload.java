package com.marketflow.pedido.event.dto;

import java.math.BigDecimal;
import java.util.List;

public record PedidoCriadoPayload(
        String pedidoId,
        String clienteId,
        String telefoneCliente,
        String operadorId,
        BigDecimal valorTotal,
        List<ItemPedidoDto> itens
) {

    public PedidoCriadoPayload {
        itens = itens == null ? List.of() : List.copyOf(itens);
    }
}
