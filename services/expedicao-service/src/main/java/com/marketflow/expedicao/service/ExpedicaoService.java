package com.marketflow.expedicao.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.marketflow.expedicao.domain.EventoProcessado;
import com.marketflow.expedicao.domain.Separacao;
import com.marketflow.expedicao.event.EnvelopeEvento;
import com.marketflow.expedicao.event.TipoEvento;
import com.marketflow.expedicao.event.dto.NotaEmitidaPayload;
import com.marketflow.expedicao.event.dto.SeparacaoPedidoIniciadoPayload;
import com.marketflow.expedicao.event.publisher.PublicadorEventos;
import com.marketflow.expedicao.repository.EventoProcessadoRepository;
import com.marketflow.expedicao.repository.SeparacaoRepository;

@Service
public class ExpedicaoService {

    private final SeparacaoRepository separacaoRepository;
    private final EventoProcessadoRepository eventoProcessadoRepository;
    private final PublicadorEventos publicadorEventos;

    public ExpedicaoService(
            SeparacaoRepository separacaoRepository,
            EventoProcessadoRepository eventoProcessadoRepository,
            PublicadorEventos publicadorEventos
    ) {
        this.separacaoRepository = separacaoRepository;
        this.eventoProcessadoRepository = eventoProcessadoRepository;
        this.publicadorEventos = publicadorEventos;
    }

    @Transactional
    public void iniciarSeparacao(EnvelopeEvento<NotaEmitidaPayload> evento) {
        if (eventoProcessadoRepository.existsById(evento.eventId())) {
            return;
        }

        String pedidoId = evento.payload().pedidoId();
        Separacao separacao = separacaoRepository.findByPedidoId(pedidoId).orElse(null);
        if (separacao == null) {
            separacao = separacaoRepository.save(new Separacao(pedidoId));
            publicadorEventos.publicar(EnvelopeEvento.create(
                    TipoEvento.SEPARACAO_PEDIDO_INICIADO,
                    evento.sagaId(),
                    evento.correlationId(),
                    new SeparacaoPedidoIniciadoPayload(
                            pedidoId,
                            separacao.getStatus().name(),
                            separacao.getIniciadaEm()
                    )
            ));
        }

        eventoProcessadoRepository.save(new EventoProcessado(evento.eventId(), evento.eventType()));
    }
}
