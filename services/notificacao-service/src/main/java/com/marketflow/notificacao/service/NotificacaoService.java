package com.marketflow.notificacao.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.marketflow.notificacao.domain.EventoProcessado;
import com.marketflow.notificacao.domain.Notificacao;
import com.marketflow.notificacao.domain.StatusNotificacao;
import com.marketflow.notificacao.event.EnvelopeEvento;
import com.marketflow.notificacao.event.dto.NotaCanceladaPayload;
import com.marketflow.notificacao.event.dto.NotaEmitidaPayload;
import com.marketflow.notificacao.event.dto.PedidoCriadoPayload;
import com.marketflow.notificacao.event.dto.SeparacaoPedidoIniciadoPayload;
import com.marketflow.notificacao.repository.EventoProcessadoRepository;
import com.marketflow.notificacao.repository.NotificacaoRepository;

@Service
public class NotificacaoService {

    private static final Logger log = LoggerFactory.getLogger(NotificacaoService.class);
    private static final String CANAL_SMS = "SMS";

    private final NotificacaoRepository notificacaoRepository;
    private final EventoProcessadoRepository eventoProcessadoRepository;

    public NotificacaoService(
            NotificacaoRepository notificacaoRepository,
            EventoProcessadoRepository eventoProcessadoRepository
    ) {
        this.notificacaoRepository = notificacaoRepository;
        this.eventoProcessadoRepository = eventoProcessadoRepository;
    }

    @Transactional
    public void processarPedidoCriado(EnvelopeEvento<PedidoCriadoPayload> evento) {
        criarSeInedita(
                evento,
                evento.payload().pedidoId(),
                "Seu pedido foi aceito."
        );
    }

    @Transactional
    public void processarNotaEmitida(EnvelopeEvento<NotaEmitidaPayload> evento) {
        criarSeInedita(
                evento,
                evento.payload().pedidoId(),
                "Sua nota fiscal foi emitida. N\u00famero da nota: " + evento.payload().numeroNota()
        );
    }

    @Transactional
    public void processarNotaCancelada(EnvelopeEvento<NotaCanceladaPayload> evento) {
        criarSeInedita(
                evento,
                evento.payload().pedidoId(),
                "Sua nota fiscal foi cancelada. Motivo: " + evento.payload().motivo()
        );
    }

    @Transactional
    public void processarSeparacaoPedidoIniciado(EnvelopeEvento<SeparacaoPedidoIniciadoPayload> evento) {
        criarSeInedita(
                evento,
                evento.payload().pedidoId(),
                "Seu pedido come\u00e7ou a ser separado."
        );
    }

    private void criarSeInedita(EnvelopeEvento<?> evento, String pedidoId, String mensagem) {
        if (eventoProcessadoRepository.existsById(evento.eventId())) {
            return;
        }

        Notificacao notificacao = notificacaoRepository.save(new Notificacao(
                pedidoId,
                evento.eventType(),
                mensagem,
                CANAL_SMS,
                StatusNotificacao.ENVIADA
        ));
        log.info(
                "SMS simulado enviado para o pedido {}: {}",
                notificacao.getPedidoId(),
                notificacao.getMensagem()
        );
        eventoProcessadoRepository.save(new EventoProcessado(evento.eventId(), evento.eventType()));
    }
}
