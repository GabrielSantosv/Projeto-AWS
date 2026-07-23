package com.marketflow.pedido.exception;

public class SessaoCaixaInvalidaException extends RuntimeException {

    public SessaoCaixaInvalidaException() {
        super("Sessao de caixa invalida ou inexistente");
    }
}
