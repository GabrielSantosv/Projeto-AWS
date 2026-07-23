package com.marketflow.pedido.event.listener;

import org.springframework.stereotype.Component;

import com.marketflow.pedido.event.EnvelopeEvento;
import com.marketflow.pedido.event.dto.EstoqueInsuficientePayload;
import com.marketflow.pedido.service.PedidoService;

@Component
public class OuvinteEventosSaga {

    private final PedidoService pedidoService;

    public OuvinteEventosSaga(PedidoService pedidoService) {
        this.pedidoService = pedidoService;
    }

    public void consumirEstoqueInsuficiente(EnvelopeEvento<EstoqueInsuficientePayload> evento) {
        pedidoService.processarEstoqueInsuficiente(evento);
    }
}
