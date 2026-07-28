package com.marketflow.expedicao.controller;

import java.time.Instant;
import java.util.UUID;

import com.marketflow.expedicao.domain.Separacao;

public final class SeparacaoDtos {

    private SeparacaoDtos() {
    }

    public record Resposta(
            UUID id,
            String pedidoId,
            String status,
            Instant iniciadaEm,
            Instant concluidaEm
    ) {
        public static Resposta from(Separacao separacao) {
            return new Resposta(
                    separacao.getId(),
                    separacao.getPedidoId(),
                    separacao.getStatus().name(),
                    separacao.getIniciadaEm(),
                    separacao.getConcluidaEm()
            );
        }
    }
}
