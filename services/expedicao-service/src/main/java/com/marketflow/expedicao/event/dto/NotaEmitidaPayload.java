package com.marketflow.expedicao.event.dto;

import java.math.BigDecimal;

public record NotaEmitidaPayload(String pedidoId, String numeroNota, BigDecimal valorTotal) {
}
