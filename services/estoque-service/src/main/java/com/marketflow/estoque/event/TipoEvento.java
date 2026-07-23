package com.marketflow.estoque.event;

public final class TipoEvento {

    public static final String PEDIDO_CRIADO = "PedidoCriado";
    public static final String ESTOQUE_ATUALIZADO = "EstoqueAtualizado";
    public static final String ESTOQUE_INSUFICIENTE = "EstoqueInsuficiente";

    private TipoEvento() {
    }
}
