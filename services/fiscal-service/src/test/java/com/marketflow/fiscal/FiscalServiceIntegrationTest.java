package com.marketflow.fiscal;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.ActiveProfiles;

import com.marketflow.fiscal.domain.NotaFiscal;
import com.marketflow.fiscal.domain.StatusNotaFiscal;
import com.marketflow.fiscal.event.EnvelopeEvento;
import com.marketflow.fiscal.event.TipoEvento;
import com.marketflow.fiscal.event.dto.EstoqueInsuficientePayload;
import com.marketflow.fiscal.event.dto.ItemPedidoDto;
import com.marketflow.fiscal.event.dto.PedidoCriadoPayload;
import com.marketflow.fiscal.event.publisher.PublicadorEventos;
import com.marketflow.fiscal.repository.EventoProcessadoRepository;
import com.marketflow.fiscal.repository.NotaFiscalRepository;
import com.marketflow.fiscal.repository.PedidoCanceladoRepository;
import com.marketflow.fiscal.service.FiscalService;

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
    private CountingPublicadorEventos publicadorEventos;

    @BeforeEach
    void setUp() {
        publicadorEventos.reset();
        notaFiscalRepository.deleteAll();
        pedidoCanceladoRepository.deleteAll();
        eventoProcessadoRepository.deleteAll();
    }

    @Test
    void pedidoCriadoEmiteNotaFiscal() {
        EnvelopeEvento<PedidoCriadoPayload> evento = EnvelopeEvento.create(
                TipoEvento.PEDIDO_CRIADO,
                "pedido-1",
                "corr-1",
                new PedidoCriadoPayload("pedido-1", "cliente-1", "11999990000", "operador-1", new BigDecimal("100.00"), List.of(new ItemPedidoDto("produto-1", 2)))
        );

        fiscalService.emitirNotaFiscal(evento);

        NotaFiscal nota = notaFiscalRepository.findByPedidoId("pedido-1").orElseThrow();
        assertThat(nota.getStatus()).isEqualTo(StatusNotaFiscal.EMITIDA);
        assertThat(nota.getValorTotal()).isEqualByComparingTo("100.00");
        assertThat(pedidoCanceladoRepository.findByPedidoId("pedido-1")).isEmpty();
        assertThat(publicadorEventos.getChamadas()).isEqualTo(1);
        assertThat(publicadorEventos.getUltimoEvento().eventType()).isEqualTo(TipoEvento.NOTA_EMITIDA);
    }

    @Test
    void estoqueInsuficienteDepoisDaNotaEmiteCancelamento() {
        EnvelopeEvento<PedidoCriadoPayload> pedidoCriado = EnvelopeEvento.create(
                TipoEvento.PEDIDO_CRIADO,
                "pedido-2",
                "corr-2",
                new PedidoCriadoPayload("pedido-2", "cliente-2", "11999990001", "operador-2", new BigDecimal("50.00"), List.of(new ItemPedidoDto("produto-2", 1)))
        );
        fiscalService.emitirNotaFiscal(pedidoCriado);

        EnvelopeEvento<EstoqueInsuficientePayload> estoqueInsuficiente = EnvelopeEvento.create(
                TipoEvento.ESTOQUE_INSUFICIENTE,
                "pedido-2",
                "corr-2",
                new EstoqueInsuficientePayload("pedido-2", "FALTA_DE_ESTOQUE", List.of(new ItemPedidoDto("produto-2", 1)), List.of(new ItemPedidoDto("produto-2", 1)))
        );
        fiscalService.cancelarPorEstoqueInsuficiente(estoqueInsuficiente);

        NotaFiscal nota = notaFiscalRepository.findByPedidoId("pedido-2").orElseThrow();
        assertThat(nota.getStatus()).isEqualTo(StatusNotaFiscal.CANCELADA);
        assertThat(pedidoCanceladoRepository.findByPedidoId("pedido-2")).isPresent();
        assertThat(publicadorEventos.getChamadas()).isEqualTo(2);
        assertThat(publicadorEventos.getUltimoEvento().eventType()).isEqualTo(TipoEvento.NOTA_CANCELADA);
    }

    @Test
    void estoqueInsuficienteAntesDoPedidoCriadoMarcaPedidoComoCancelado() {
        EnvelopeEvento<EstoqueInsuficientePayload> estoqueInsuficiente = EnvelopeEvento.create(
                TipoEvento.ESTOQUE_INSUFICIENTE,
                "pedido-3",
                "corr-3",
                new EstoqueInsuficientePayload("pedido-3", "FALTA_DE_ESTOQUE", List.of(new ItemPedidoDto("produto-3", 1)), List.of(new ItemPedidoDto("produto-3", 1)))
        );
        fiscalService.cancelarPorEstoqueInsuficiente(estoqueInsuficiente);

        EnvelopeEvento<PedidoCriadoPayload> pedidoCriado = EnvelopeEvento.create(
                TipoEvento.PEDIDO_CRIADO,
                "pedido-3",
                "corr-3",
                new PedidoCriadoPayload("pedido-3", "cliente-3", "11999990002", "operador-3", new BigDecimal("30.00"), List.of(new ItemPedidoDto("produto-3", 1)))
        );
        fiscalService.emitirNotaFiscal(pedidoCriado);

        assertThat(notaFiscalRepository.findByPedidoId("pedido-3")).isEmpty();
        assertThat(pedidoCanceladoRepository.findByPedidoId("pedido-3")).isPresent();
        assertThat(publicadorEventos.getChamadas()).isZero();
    }

    @Test
    void pedidoCriadoDuplicadoEhIdempotente() {
        EnvelopeEvento<PedidoCriadoPayload> evento = EnvelopeEvento.create(
                TipoEvento.PEDIDO_CRIADO,
                "pedido-4",
                "corr-4",
                new PedidoCriadoPayload("pedido-4", "cliente-4", "11999990003", "operador-4", new BigDecimal("80.00"), List.of(new ItemPedidoDto("produto-4", 1)))
        );

        fiscalService.emitirNotaFiscal(evento);
        fiscalService.emitirNotaFiscal(evento);

        assertThat(notaFiscalRepository.findAll()).hasSize(1);
        assertThat(eventoProcessadoRepository.findAll()).hasSize(1);
        assertThat(publicadorEventos.getChamadas()).isEqualTo(1);
    }

    @Test
    void estoqueInsuficienteSemPedidoCriadoRegistraCancelamentoSemPublicarNotaCancelada() {
        EnvelopeEvento<EstoqueInsuficientePayload> evento = EnvelopeEvento.create(
                TipoEvento.ESTOQUE_INSUFICIENTE,
                "pedido-5",
                "corr-5",
                new EstoqueInsuficientePayload("pedido-5", "FALTA_DE_ESTOQUE", List.of(new ItemPedidoDto("produto-5", 1)), List.of(new ItemPedidoDto("produto-5", 1)))
        );

        fiscalService.cancelarPorEstoqueInsuficiente(evento);

        assertThat(pedidoCanceladoRepository.findByPedidoId("pedido-5")).isPresent();
        assertThat(notaFiscalRepository.findByPedidoId("pedido-5")).isEmpty();
        assertThat(publicadorEventos.getChamadas()).isZero();
    }

    @Test
    void estoqueInsuficienteDuplicadoEhIdempotente() {
        EnvelopeEvento<EstoqueInsuficientePayload> evento = EnvelopeEvento.create(
                TipoEvento.ESTOQUE_INSUFICIENTE,
                "pedido-6",
                "corr-6",
                new EstoqueInsuficientePayload("pedido-6", "FALTA_DE_ESTOQUE", List.of(new ItemPedidoDto("produto-6", 1)), List.of(new ItemPedidoDto("produto-6", 1)))
        );

        fiscalService.cancelarPorEstoqueInsuficiente(evento);
        fiscalService.cancelarPorEstoqueInsuficiente(evento);

        assertThat(pedidoCanceladoRepository.findAll()).hasSize(1);
        assertThat(eventoProcessadoRepository.findAll()).hasSize(1);
        assertThat(publicadorEventos.getChamadas()).isZero();
    }

    @org.springframework.boot.SpringBootConfiguration
    static class TestConfig {

        @Bean
        @Primary
        PublicadorEventos publicadorEventos(CountingPublicadorEventos countingPublicadorEventos) {
            return countingPublicadorEventos;
        }

        @Bean
        CountingPublicadorEventos countingPublicadorEventos() {
            return new CountingPublicadorEventos();
        }
    }

    static class CountingPublicadorEventos extends PublicadorEventos {
        private int chamadas;
        private EnvelopeEvento<?> ultimoEvento;

        @Override
        public void publicar(EnvelopeEvento<?> evento) {
            chamadas++;
            ultimoEvento = evento;
        }

        int getChamadas() {
            return chamadas;
        }

        EnvelopeEvento<?> getUltimoEvento() {
            return ultimoEvento;
        }

        void reset() {
            chamadas = 0;
            ultimoEvento = null;
        }
    }
}
