package com.marketflow.estoque.event.dto;

import java.util.List;

public record PedidoCriadoPayload(
        String pedidoId,
        String clienteId,
        String telefoneCliente,
        List<ItemPedido> itens
) {

    public PedidoCriadoPayload {
        itens = itens == null ? List.of() : List.copyOf(itens);
    }
}
