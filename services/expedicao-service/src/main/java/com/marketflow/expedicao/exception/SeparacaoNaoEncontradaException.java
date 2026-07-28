package com.marketflow.expedicao.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.NOT_FOUND)
public class SeparacaoNaoEncontradaException extends RuntimeException {

    public SeparacaoNaoEncontradaException(String pedidoId) {
        super("Separacao nao encontrada para pedido: " + pedidoId);
    }
}
