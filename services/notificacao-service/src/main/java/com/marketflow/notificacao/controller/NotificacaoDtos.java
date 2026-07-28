package com.marketflow.notificacao.controller;

import java.time.Instant;
import java.util.UUID;

import com.marketflow.notificacao.domain.Notificacao;

public final class NotificacaoDtos {

    private NotificacaoDtos() {
    }

    public record Resposta(
            UUID id,
            String pedidoId,
            String tipoEvento,
            String mensagem,
            String canal,
            String status,
            Instant enviadaEm
    ) {
        public static Resposta from(Notificacao notificacao) {
            return new Resposta(
                    notificacao.getId(),
                    notificacao.getPedidoId(),
                    notificacao.getTipoEvento(),
                    notificacao.getMensagem(),
                    notificacao.getCanal(),
                    notificacao.getStatus().name(),
                    notificacao.getEnviadaEm()
            );
        }
    }
}
