package com.marketflow.pedido.exception;

import java.util.UUID;

public class PedidoNaoEncontradoException extends RuntimeException {

    public PedidoNaoEncontradoException(UUID pedidoId) {
        super("Pedido nao encontrado: " + pedidoId);
    }
}
