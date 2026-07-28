package com.marketflow.expedicao.event.dto;

import java.time.Instant;

public record SeparacaoPedidoIniciadoPayload(
        String pedidoId,
        String status,
        Instant iniciadaEm
) {
}
