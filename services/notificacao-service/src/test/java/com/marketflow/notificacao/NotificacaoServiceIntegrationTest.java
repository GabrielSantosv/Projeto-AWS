package com.marketflow.notificacao;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import com.marketflow.notificacao.domain.Notificacao;
import com.marketflow.notificacao.domain.StatusNotificacao;
import com.marketflow.notificacao.event.EnvelopeEvento;
import com.marketflow.notificacao.event.TipoEvento;
import com.marketflow.notificacao.event.dto.NotaCanceladaPayload;
import com.marketflow.notificacao.event.dto.NotaEmitidaPayload;
import com.marketflow.notificacao.event.dto.PedidoCriadoPayload;
import com.marketflow.notificacao.event.dto.SeparacaoPedidoIniciadoPayload;
import com.marketflow.notificacao.repository.EventoProcessadoRepository;
import com.marketflow.notificacao.repository.NotificacaoRepository;
import com.marketflow.notificacao.service.NotificacaoService;

@SpringBootTest
@ActiveProfiles("test")
class NotificacaoServiceIntegrationTest {

    @Autowired
    private NotificacaoService notificacaoService;

    @Autowired
    private NotificacaoRepository notificacaoRepository;

    @Autowired
    private EventoProcessadoRepository eventoProcessadoRepository;

    @BeforeEach
    void setUp() {
        notificacaoRepository.deleteAll();
        eventoProcessadoRepository.deleteAll();
    }

    @Test
    void pedidoCriadoGeraNotificacaoDePedidoAceito() {
        notificacaoService.processarPedidoCriado(pedidoCriado("evt-pedido", "pedido-1"));

        Notificacao notificacao = notificacaoUnica("pedido-1");
        assertThat(notificacao.getTipoEvento()).isEqualTo(TipoEvento.PEDIDO_CRIADO);
        assertThat(notificacao.getMensagem()).isEqualTo("Seu pedido foi aceito.");
        assertThat(notificacao.getCanal()).isEqualTo("SMS");
        assertThat(notificacao.getStatus()).isEqualTo(StatusNotificacao.ENVIADA);
    }

    @Test
    void notaEmitidaGeraNotificacaoComNumeroDaNota() {
        notificacaoService.processarNotaEmitida(notaEmitida("evt-nota", "pedido-2", "NF-2026-2"));

        Notificacao notificacao = notificacaoUnica("pedido-2");
        assertThat(notificacao.getTipoEvento()).isEqualTo(TipoEvento.NOTA_EMITIDA);
        assertThat(notificacao.getMensagem()).contains("nota fiscal foi emitida", "NF-2026-2");
    }

    @Test
    void notaCanceladaGeraNotificacaoComMotivo() {
        notificacaoService.processarNotaCancelada(notaCancelada("evt-cancelada", "pedido-3"));

        Notificacao notificacao = notificacaoUnica("pedido-3");
        assertThat(notificacao.getTipoEvento()).isEqualTo(TipoEvento.NOTA_CANCELADA);
        assertThat(notificacao.getMensagem()).contains("nota fiscal foi cancelada", "ESTOQUE_INSUFICIENTE");
    }

    @Test
    void separacaoIniciadaGeraNotificacaoDeInicioDaSeparacao() {
        notificacaoService.processarSeparacaoPedidoIniciado(separacaoIniciada("evt-separacao", "pedido-4"));

        Notificacao notificacao = notificacaoUnica("pedido-4");
        assertThat(notificacao.getTipoEvento()).isEqualTo(TipoEvento.SEPARACAO_PEDIDO_INICIADO);
        assertThat(notificacao.getMensagem()).isEqualTo("Seu pedido come\u00e7ou a ser separado.");
    }

    @Test
    void cadaTipoDeEventoRepetidoEhIdempotente() {
        EnvelopeEvento<PedidoCriadoPayload> pedido = pedidoCriado("evt-dup-pedido", "pedido-dup");
        EnvelopeEvento<NotaEmitidaPayload> nota = notaEmitida("evt-dup-nota", "pedido-dup", "NF-DUP");
        EnvelopeEvento<NotaCanceladaPayload> cancelamento = notaCancelada("evt-dup-cancelamento", "pedido-dup");
        EnvelopeEvento<SeparacaoPedidoIniciadoPayload> separacao = separacaoIniciada(
                "evt-dup-separacao",
                "pedido-dup"
        );

        notificacaoService.processarPedidoCriado(pedido);
        notificacaoService.processarPedidoCriado(pedido);
        notificacaoService.processarNotaEmitida(nota);
        notificacaoService.processarNotaEmitida(nota);
        notificacaoService.processarNotaCancelada(cancelamento);
        notificacaoService.processarNotaCancelada(cancelamento);
        notificacaoService.processarSeparacaoPedidoIniciado(separacao);
        notificacaoService.processarSeparacaoPedidoIniciado(separacao);

        assertThat(notificacaoRepository.findByPedidoIdOrderByEnviadaEmAsc("pedido-dup")).hasSize(4);
        assertThat(eventoProcessadoRepository.findAll()).hasSize(4);
    }

    @Test
    void eventosDiferentesDoMesmoPedidoGeramNotificacoesDistintas() {
        notificacaoService.processarPedidoCriado(pedidoCriado("evt-mesmo-1", "pedido-mesmo"));
        notificacaoService.processarNotaEmitida(notaEmitida("evt-mesmo-2", "pedido-mesmo", "NF-MESMO"));

        List<Notificacao> notificacoes = notificacaoRepository
                .findByPedidoIdOrderByEnviadaEmAsc("pedido-mesmo");
        assertThat(notificacoes).hasSize(2);
        assertThat(notificacoes).extracting(Notificacao::getTipoEvento)
                .containsExactly(TipoEvento.PEDIDO_CRIADO, TipoEvento.NOTA_EMITIDA);
    }

    private Notificacao notificacaoUnica(String pedidoId) {
        return notificacaoRepository.findByPedidoIdOrderByEnviadaEmAsc(pedidoId)
                .stream()
                .findFirst()
                .orElseThrow();
    }

    private EnvelopeEvento<PedidoCriadoPayload> pedidoCriado(String eventId, String pedidoId) {
        return evento(
                eventId,
                TipoEvento.PEDIDO_CRIADO,
                pedidoId,
                new PedidoCriadoPayload(pedidoId, "cliente-1", "+5511999999999")
        );
    }

    private EnvelopeEvento<NotaEmitidaPayload> notaEmitida(String eventId, String pedidoId, String numeroNota) {
        return evento(
                eventId,
                TipoEvento.NOTA_EMITIDA,
                pedidoId,
                new NotaEmitidaPayload(pedidoId, numeroNota, new BigDecimal("100.00"))
        );
    }

    private EnvelopeEvento<NotaCanceladaPayload> notaCancelada(String eventId, String pedidoId) {
        return evento(
                eventId,
                TipoEvento.NOTA_CANCELADA,
                pedidoId,
                new NotaCanceladaPayload(pedidoId, "NF-CANCELADA", "ESTOQUE_INSUFICIENTE")
        );
    }

    private EnvelopeEvento<SeparacaoPedidoIniciadoPayload> separacaoIniciada(String eventId, String pedidoId) {
        return evento(
                eventId,
                TipoEvento.SEPARACAO_PEDIDO_INICIADO,
                pedidoId,
                new SeparacaoPedidoIniciadoPayload(pedidoId, "INICIADA")
        );
    }

    private <T> EnvelopeEvento<T> evento(String eventId, String eventType, String pedidoId, T payload) {
        EnvelopeEvento<T> criado = EnvelopeEvento.create(eventType, pedidoId, "corr-" + pedidoId, payload);
        return new EnvelopeEvento<>(
                eventId,
                criado.eventType(),
                criado.sagaId(),
                criado.correlationId(),
                criado.timestamp(),
                criado.version(),
                criado.payload()
        );
    }
}
