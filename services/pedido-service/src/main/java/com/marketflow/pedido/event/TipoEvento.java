package com.marketflow.pedido.event;

public final class TipoEvento {

    public static final String PEDIDO_CRIADO = "PedidoCriado";
    public static final String ESTOQUE_INSUFICIENTE = "EstoqueInsuficiente";
    public static final String SEPARACAO_PEDIDO_INICIADO = "SeparacaoPedidoIniciado";

    private TipoEvento() {
    }
}
