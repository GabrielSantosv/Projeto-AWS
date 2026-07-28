package com.marketflow.expedicao;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;

import com.marketflow.expedicao.domain.EventoProcessado;
import com.marketflow.expedicao.domain.OutboxEvent;
import com.marketflow.expedicao.domain.Separacao;
import com.marketflow.expedicao.domain.StatusSeparacao;
import com.marketflow.expedicao.event.EnvelopeEvento;
import com.marketflow.expedicao.event.TipoEvento;
import com.marketflow.expedicao.event.dto.NotaEmitidaPayload;
import com.marketflow.expedicao.event.listener.OuvinteEventosSaga;
import com.marketflow.expedicao.event.publisher.OutboxPublisher;
import com.marketflow.expedicao.repository.EventoProcessadoRepository;
import com.marketflow.expedicao.repository.OutboxEventRepository;
import com.marketflow.expedicao.repository.SeparacaoRepository;
import com.marketflow.expedicao.service.ExpedicaoService;

import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sns.model.PublishRequest;

@SpringBootTest
@ActiveProfiles("test")
class ExpedicaoServiceIntegrationTest {

    @Autowired
    private ExpedicaoService expedicaoService;

    @Autowired
    private SeparacaoRepository separacaoRepository;

    @Autowired
    private EventoProcessadoRepository eventoProcessadoRepository;

    @Autowired
    private OutboxEventRepository outboxEventRepository;

    @Autowired
    private OutboxPublisher outboxPublisher;

    @Autowired
    private OuvinteEventosSaga ouvinteEventosSaga;

    @MockBean
    private SnsClient snsClient;

    @BeforeEach
    void setUp() {
        outboxEventRepository.deleteAll();
        separacaoRepository.deleteAll();
        eventoProcessadoRepository.deleteAll();
    }

    @Test
    void notaEmitidaCriaSeparacaoEOutbox() {
        expedicaoService.iniciarSeparacao(notaEmitida("evt-1", "pedido-1"));

        Separacao separacao = separacaoRepository.findByPedidoId("pedido-1").orElseThrow();
        assertThat(separacao.getStatus()).isEqualTo(StatusSeparacao.INICIADA);
        OutboxEvent outbox = outboxEventRepository.findAll().stream().findFirst().orElseThrow();
        assertThat(outbox.getEventType()).isEqualTo(TipoEvento.SEPARACAO_PEDIDO_INICIADO);
        assertThat(outbox.isPublicado()).isFalse();
        assertThat(eventoProcessadoRepository.findAll()).hasSize(1);
    }

    @Test
    void notaEmitidaDuplicadaEhIdempotente() {
        EnvelopeEvento<NotaEmitidaPayload> evento = notaEmitida("evt-duplicado", "pedido-2");

        expedicaoService.iniciarSeparacao(evento);
        expedicaoService.iniciarSeparacao(evento);

        assertThat(separacaoRepository.findAll()).hasSize(1);
        assertThat(outboxEventRepository.findAll()).hasSize(1);
        assertThat(eventoProcessadoRepository.findAll()).hasSize(1);
    }

    @Test
    void outboxPublicaMarcaComoPublicadoEnaoRepublica() {
        expedicaoService.iniciarSeparacao(notaEmitida("evt-3", "pedido-3"));

        outboxPublisher.publicarPendentesAgora();
        outboxPublisher.publicarPendentesAgora();

        OutboxEvent outbox = outboxEventRepository.findAll().stream().findFirst().orElseThrow();
        assertThat(outbox.isPublicado()).isTrue();
        assertThat(outbox.getPublicadoEm()).isNotNull();
        verify(snsClient, times(1)).publish(any(PublishRequest.class));
    }

    @Test
    void expedicaoNaoTrataPedidoCriado() {
        assertThat(ouvinteEventosSaga.tiposTratados())
                .containsExactly(TipoEvento.NOTA_EMITIDA)
                .doesNotContain(TipoEvento.PEDIDO_CRIADO);
    }

    private EnvelopeEvento<NotaEmitidaPayload> notaEmitida(String eventId, String pedidoId) {
        EnvelopeEvento<NotaEmitidaPayload> criado = EnvelopeEvento.create(
                TipoEvento.NOTA_EMITIDA,
                pedidoId,
                "corr-" + pedidoId,
                new NotaEmitidaPayload(pedidoId, "NF-" + pedidoId, new BigDecimal("80.00"))
        );
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
