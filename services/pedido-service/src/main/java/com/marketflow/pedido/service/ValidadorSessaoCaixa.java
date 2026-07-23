package com.marketflow.pedido.service;

import org.springframework.stereotype.Component;

public interface ValidadorSessaoCaixa {

    boolean vendaAtiva(String operadorId, String tokenSessaoCaixa);
}

@Component("validadorSessaoCaixaStub")
class ValidadorSessaoCaixaStub implements ValidadorSessaoCaixa {

    @Override
    public boolean vendaAtiva(String operadorId, String tokenSessaoCaixa) {
        // TODO: substituir pela validacao real quando o funcionario-service existir.
        return true;
    }
}
