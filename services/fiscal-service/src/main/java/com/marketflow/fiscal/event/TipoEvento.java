package com.marketflow.fiscal.event;

public final class TipoEvento {

    public static final String PEDIDO_CRIADO = "PedidoCriado";
    public static final String ESTOQUE_INSUFICIENTE = "EstoqueInsuficiente";
    public static final String NOTA_EMITIDA = "NotaEmitida";
    public static final String NOTA_CANCELADA = "NotaCancelada";

    private TipoEvento() {
    }
}
