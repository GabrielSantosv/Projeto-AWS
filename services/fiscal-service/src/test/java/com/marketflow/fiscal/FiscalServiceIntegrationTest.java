package com.marketflow.fiscal;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import com.marketflow.fiscal.domain.NotaFiscal;
import com.marketflow.fiscal.domain.OutboxEvent;
import com.marketflow.fiscal.domain.StatusNotaFiscal;
import com.marketflow.fiscal.event.EnvelopeEvento;
import com.marketflow.fiscal.event.TipoEvento;
import com.marketflow.fiscal.event.dto.EstoqueInsuficientePayload;
import com.marketflow.fiscal.event.dto.ItemPedidoDto;
import com.marketflow.fiscal.event.dto.PedidoCriadoPayload;
import com.marketflow.fiscal.event.publisher.OutboxPublisher;
import com.marketflow.fiscal.event.publisher.PublicadorBroker;
import com.marketflow.fiscal.repository.EventoProcessadoRepository;
import com.marketflow.fiscal.repository.NotaFiscalRepository;
import com.marketflow.fiscal.repository.OutboxEventRepository;
import com.marketflow.fiscal.repository.PedidoCanceladoRepository;
import com.marketflow.fiscal.service.FiscalService;

import software.amazon.awssdk.services.sns.SnsClient;

@SpringBootTest(classes = {FiscalServiceApplication.class, FiscalServiceIntegrationTest.TestConfig.class})
@ActiveProfiles("test")
class FiscalServiceIntegrationTest {

    @Autowired
    private FiscalService fiscalService;

    @Autowired
    private NotaFiscalRepository notaFiscalRepository;

    @Autowired
    private PedidoCanceladoRepository pedidoCanceladoRepository;

    @Autowired
    private EventoProcessadoRepository eventoProcessadoRepository;

    @Autowired
    private OutboxEventRepository outboxEventRepository;

    @Autowired
    private OutboxPublisher outboxPublisher;

    @Autowired
    private CountingPublicadorBroker publicadorBroker;

    @BeforeEach
    void setUp() {
        publicadorBroker.reset();
        notaFiscalRepository.deleteAll();
        pedidoCanceladoRepository.deleteAll();
        eventoProcessadoRepository.deleteAll();
        outboxEventRepository.deleteAll();
    }

    @Test
    void pedidoCriadoEmiteNotaFiscalEGravaOutbox() {
        fiscalService.emitirNotaFiscal(pedidoCriado("pedido-1", "corr-1"));

        NotaFiscal nota = notaFiscalRepository.findByPedidoId("pedido-1").orElseThrow();
        assertThat(nota.getStatus()).isEqualTo(StatusNotaFiscal.EMITIDA);
        assertThat(nota.getValorTotal()).isEqualByComparingTo("100.00");
        assertThat(outboxEventRepository.findAll()).singleElement()
                .extracting(OutboxEvent::getEventType).isEqualTo(TipoEvento.NOTA_EMITIDA);
        assertThat(publicadorBroker.getChamadas()).isZero();
    }

    @Test
    void estoqueInsuficienteDepoisDaNotaGravaCancelamentoNoOutbox() {
        fiscalService.emitirNotaFiscal(pedidoCriado("pedido-2", "corr-2"));
        fiscalService.cancelarPorEstoqueInsuficiente(estoqueInsuficiente("pedido-2", "corr-2"));

        NotaFiscal nota = notaFiscalRepository.findByPedidoId("pedido-2").orElseThrow();
        assertThat(nota.getStatus()).isEqualTo(StatusNotaFiscal.CANCELADA);
        assertThat(pedidoCanceladoRepository.findByPedidoId("pedido-2")).isPresent();
        assertThat(outboxEventRepository.findAll()).extracting(OutboxEvent::getEventType)
                .containsExactlyInAnyOrder(TipoEvento.NOTA_EMITIDA, TipoEvento.NOTA_CANCELADA);
    }

    @Test
    void estoqueInsuficienteAntesDoPedidoCriadoMarcaPedidoComoCancelado() {
        fiscalService.cancelarPorEstoqueInsuficiente(estoqueInsuficiente("pedido-3", "corr-3"));
        fiscalService.emitirNotaFiscal(pedidoCriado("pedido-3", "corr-3"));

        assertThat(notaFiscalRepository.findByPedidoId("pedido-3")).isEmpty();
        assertThat(pedidoCanceladoRepository.findByPedidoId("pedido-3")).isPresent();
        assertThat(outboxEventRepository).isNotNull();
        assertThat(outboxEventRepository.findAll()).isEmpty();
    }

    @Test
    void pedidoCriadoDuplicadoEhIdempotente() {
        EnvelopeEvento<PedidoCriadoPayload> evento = pedidoCriado("pedido-4", "corr-4");
        fiscalService.emitirNotaFiscal(evento);
        fiscalService.emitirNotaFiscal(evento);

        assertThat(notaFiscalRepository.findAll()).hasSize(1);
        assertThat(eventoProcessadoRepository.findAll()).hasSize(1);
        assertThat(outboxEventRepository.findAll()).hasSize(1);
    }

    @Test
    void estoqueInsuficienteSemPedidoCriadoRegistraCancelamentoSemPublicarNotaCancelada() {
        fiscalService.cancelarPorEstoqueInsuficiente(estoqueInsuficiente("pedido-5", "corr-5"));

        assertThat(pedidoCanceladoRepository.findByPedidoId("pedido-5")).isPresent();
        assertThat(notaFiscalRepository.findByPedidoId("pedido-5")).isEmpty();
        assertThat(outboxEventRepository.findAll()).isEmpty();
    }

    @Test
    void estoqueInsuficienteDuplicadoEhIdempotente() {
        EnvelopeEvento<EstoqueInsuficientePayload> evento = estoqueInsuficiente("pedido-6", "corr-6");
        fiscalService.cancelarPorEstoqueInsuficiente(evento);
        fiscalService.cancelarPorEstoqueInsuficiente(evento);

        assertThat(pedidoCanceladoRepository.findAll()).hasSize(1);
        assertThat(eventoProcessadoRepository.findAll()).hasSize(1);
        assertThat(outboxEventRepository.findAll()).isEmpty();
    }

    @Test
    void outboxPublicaUmaVezEAtualizaStatus() {
        fiscalService.emitirNotaFiscal(pedidoCriado("pedido-7", "corr-7"));

        outboxPublisher.publicarPendentesAgora();
        outboxPublisher.publicarPendentesAgora();

        assertThat(publicadorBroker.getChamadas()).isEqualTo(1);
        assertThat(outboxEventRepository.findAll()).singleElement().extracting(OutboxEvent::isPublicado)
                .isEqualTo(true);
    }

    private EnvelopeEvento<PedidoCriadoPayload> pedidoCriado(String pedidoId, String correlationId) {
        return EnvelopeEvento.create(
                TipoEvento.PEDIDO_CRIADO,
                pedidoId,
                correlationId,
                new PedidoCriadoPayload(
                        pedidoId,
                        "cliente-" + pedidoId,
                        "11999990000",
                        "operador-1",
                        new BigDecimal("100.00"),
                        List.of(new ItemPedidoDto("produto-1", 2))
                )
        );
    }

    private EnvelopeEvento<EstoqueInsuficientePayload> estoqueInsuficiente(String pedidoId, String correlationId) {
        return EnvelopeEvento.create(
                TipoEvento.ESTOQUE_INSUFICIENTE,
                pedidoId,
                correlationId,
                new EstoqueInsuficientePayload(
                        pedidoId,
                        "FALTA_DE_ESTOQUE",
                        List.of(new ItemPedidoDto("produto-1", 1)),
                        List.of(new ItemPedidoDto("produto-1", 1))
                )
        );
    }

    @org.springframework.boot.SpringBootConfiguration
    static class TestConfig {

        @Bean
        @Primary
        PublicadorBroker publicadorBroker(CountingPublicadorBroker broker) {
            return broker;
        }

        @Bean
        CountingPublicadorBroker countingPublicadorBroker() {
            return new CountingPublicadorBroker();
        }
    }

    static class CountingPublicadorBroker extends PublicadorBroker {
        private int chamadas;

        CountingPublicadorBroker() {
            super((SnsClient) null);
        }

        @Override
        public void publicar(OutboxEvent evento) {
            chamadas++;
        }

        int getChamadas() {
            return chamadas;
        }

        void reset() {
            chamadas = 0;
        }
    }
}
