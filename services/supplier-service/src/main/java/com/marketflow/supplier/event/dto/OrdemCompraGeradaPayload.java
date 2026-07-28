package com.marketflow.supplier.event.dto;

import java.util.UUID;

public record OrdemCompraGeradaPayload(
        UUID ordemCompraId,
        String pedidoId,
        String produtoId,
        UUID fornecedorId,
        int quantidade,
        String status
) {
}
