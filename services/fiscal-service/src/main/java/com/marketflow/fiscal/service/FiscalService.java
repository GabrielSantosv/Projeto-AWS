package com.marketflow.fiscal.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketflow.fiscal.domain.EventoProcessado;
import com.marketflow.fiscal.domain.NotaFiscal;
import com.marketflow.fiscal.domain.PedidoCancelado;
import com.marketflow.fiscal.event.EnvelopeEvento;
import com.marketflow.fiscal.event.TipoEvento;
import com.marketflow.fiscal.event.dto.EstoqueInsuficientePayload;
import com.marketflow.fiscal.event.dto.NotaCanceladaPayload;
import com.marketflow.fiscal.event.dto.NotaEmitidaPayload;
import com.marketflow.fiscal.event.dto.PedidoCriadoPayload;
import com.marketflow.fiscal.event.publisher.PublicadorEventos;
import com.marketflow.fiscal.repository.EventoProcessadoRepository;
import com.marketflow.fiscal.repository.NotaFiscalRepository;
import com.marketflow.fiscal.repository.PedidoCanceladoRepository;

@Service
public class FiscalService {

    private final NotaFiscalRepository notaFiscalRepository;
    private final PedidoCanceladoRepository pedidoCanceladoRepository;
    private final EventoProcessadoRepository eventoProcessadoRepository;
    private final PublicadorEventos publicadorEventos;
    private final ObjectMapper objectMapper;

    public FiscalService(
            NotaFiscalRepository notaFiscalRepository,
            PedidoCanceladoRepository pedidoCanceladoRepository,
            EventoProcessadoRepository eventoProcessadoRepository,
            PublicadorEventos publicadorEventos,
            ObjectMapper objectMapper
    ) {
        this.notaFiscalRepository = notaFiscalRepository;
        this.pedidoCanceladoRepository = pedidoCanceladoRepository;
        this.eventoProcessadoRepository = eventoProcessadoRepository;
        this.publicadorEventos = publicadorEventos;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public void emitirNotaFiscal(EnvelopeEvento<PedidoCriadoPayload> evento) {
        if (eventoProcessadoRepository.existsById(evento.eventId())) {
            return;
        }

        PedidoCriadoPayload payload = evento.payload();
        if (pedidoCanceladoRepository.findByPedidoId(payload.pedidoId()).isPresent()) {
            registrarEventoProcessado(evento);
            return;
        }

        NotaFiscal notaExistente = notaFiscalRepository.findByPedidoId(payload.pedidoId()).orElse(null);
        if (notaExistente != null) {
            registrarEventoProcessado(evento);
            return;
        }

        NotaFiscal notaFiscal = new NotaFiscal(payload.pedidoId(), gerarNumeroNota(payload), payload.valorTotal());
        notaFiscalRepository.save(notaFiscal);

        EnvelopeEvento<NotaEmitidaPayload> eventoSaida = EnvelopeEvento.create(
                TipoEvento.NOTA_EMITIDA,
                payload.pedidoId(),
                evento.correlationId(),
                new NotaEmitidaPayload(payload.pedidoId(), notaFiscal.getNumeroNota(), notaFiscal.getValorTotal())
        );
        publicadorEventos.publicar(eventoSaida);
        registrarEventoProcessado(evento);
    }

    @Transactional
    public void cancelarPorEstoqueInsuficiente(EnvelopeEvento<EstoqueInsuficientePayload> evento) {
        if (eventoProcessadoRepository.existsById(evento.eventId())) {
            return;
        }

        String pedidoId = evento.payload().pedidoId();
        pedidoCanceladoRepository.findByPedidoId(pedidoId)
                .orElseGet(() -> pedidoCanceladoRepository.save(new PedidoCancelado(pedidoId, "ESTOQUE_INSUFICIENTE")));

        NotaFiscal notaFiscal = notaFiscalRepository.findByPedidoId(pedidoId).orElse(null);
        if (notaFiscal != null) {
            notaFiscal.cancelar();
            notaFiscalRepository.save(notaFiscal);
            EnvelopeEvento<NotaCanceladaPayload> eventoSaida = EnvelopeEvento.create(
                    TipoEvento.NOTA_CANCELADA,
                    pedidoId,
                    evento.correlationId(),
                    new NotaCanceladaPayload(pedidoId, notaFiscal.getNumeroNota(), "ESTOQUE_INSUFICIENTE")
            );
            publicadorEventos.publicar(eventoSaida);
        }

        registrarEventoProcessado(evento);
    }

    private void registrarEventoProcessado(EnvelopeEvento<?> evento) {
        eventoProcessadoRepository.save(new EventoProcessado(evento.eventId(), evento.eventType()));
    }

    private String gerarNumeroNota(PedidoCriadoPayload payload) {
        return "NF-" + payload.pedidoId().substring(0, Math.min(payload.pedidoId().length(), 8)).toUpperCase();
    }
}
