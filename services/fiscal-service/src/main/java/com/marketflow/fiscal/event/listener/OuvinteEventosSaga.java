package com.marketflow.fiscal.event.listener;

import org.springframework.stereotype.Component;

import com.marketflow.fiscal.event.EnvelopeEvento;
import com.marketflow.fiscal.event.dto.EstoqueInsuficientePayload;
import com.marketflow.fiscal.event.dto.PedidoCriadoPayload;
import com.marketflow.fiscal.service.FiscalService;

@Component
public class OuvinteEventosSaga {

    private final FiscalService fiscalService;

    public OuvinteEventosSaga(FiscalService fiscalService) {
        this.fiscalService = fiscalService;
    }

    public void consumirPedidoCriado(EnvelopeEvento<PedidoCriadoPayload> evento) {
        fiscalService.emitirNotaFiscal(evento);
    }

    public void consumirEstoqueInsuficiente(EnvelopeEvento<EstoqueInsuficientePayload> evento) {
        fiscalService.cancelarPorEstoqueInsuficiente(evento);
    }
}
