package com.marketflow.fiscal.exception;

public class NotaFiscalNaoEncontradaException extends RuntimeException {

    public NotaFiscalNaoEncontradaException(String pedidoId) {
        super("Nota fiscal nao encontrada para pedido " + pedidoId);
    }
}
