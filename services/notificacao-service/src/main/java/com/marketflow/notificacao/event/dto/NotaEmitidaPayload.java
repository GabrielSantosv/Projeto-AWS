package com.marketflow.notificacao.event.dto;

import java.math.BigDecimal;

public record NotaEmitidaPayload(String pedidoId, String numeroNota, BigDecimal valorTotal) {
}
