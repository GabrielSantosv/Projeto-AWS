package com.marketflow.estoque.exception;

public class ProdutoNaoEncontradoException extends RuntimeException {

    public ProdutoNaoEncontradoException(String produtoId) {
        super("Produto nao encontrado: " + produtoId);
    }
}
