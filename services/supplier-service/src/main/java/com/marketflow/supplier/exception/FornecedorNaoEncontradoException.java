package com.marketflow.supplier.exception;

import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.NOT_FOUND)
public class FornecedorNaoEncontradoException extends RuntimeException {

    public FornecedorNaoEncontradoException(UUID id) {
        super("Fornecedor nao encontrado: " + id);
    }
}
