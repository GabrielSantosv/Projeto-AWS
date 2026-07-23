package com.marketflow.fiscal.controller;

import java.math.BigDecimal;
import java.time.Instant;

public class NotaFiscalDtos {

    public record Resposta(
            String pedidoId,
            String numeroNota,
            BigDecimal valorTotal,
            String status,
            Instant emitidaEm
    ) {
    }
}
