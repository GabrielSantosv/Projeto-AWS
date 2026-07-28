package com.marketflow.notificacao.event.dto;

public record NotaCanceladaPayload(String pedidoId, String numeroNota, String motivo) {
}
