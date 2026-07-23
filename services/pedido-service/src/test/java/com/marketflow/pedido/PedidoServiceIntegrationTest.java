package com.marketflow.pedido;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.ActiveProfiles;

import com.marketflow.pedido.domain.OutboxEvent;
import com.marketflow.pedido.domain.Pedido;
import com.marketflow.pedido.domain.StatusPedido;
import com.marketflow.pedido.event.EnvelopeEvento;
import com.marketflow.pedido.event.TipoEvento;
import com.marketflow.pedido.event.dto.EstoqueInsuficientePayload;
import com.marketflow.pedido.event.dto.ItemPedidoDto;
import com.marketflow.pedido.event.publisher.OutboxPublisher;
import com.marketflow.pedido.event.publisher.PublicadorEventos;
import com.marketflow.pedido.exception.SessaoCaixaInvalidaException;
import com.marketflow.pedido.repository.EventoProcessadoRepository;
import com.marketflow.pedido.repository.OutboxEventRepository;
import com.marketflow.pedido.repository.PedidoRepository;
import com.marketflow.pedido.service.PedidoService;
import com.marketflow.pedido.service.ValidadorSessaoCaixa;

@SpringBootTest(classes = {PedidoServiceApplication.class, PedidoServiceIntegrationTest.TestConfig.class})
@ActiveProfiles("test")
class PedidoServiceIntegrationTest {

    @Autowired
    private PedidoService pedidoService;

    @Autowired
    private PedidoRepository pedidoRepository;

    @Autowired
    private OutboxEventRepository outboxEventRepository;

    @Autowired
    private EventoProcessadoRepository eventoProcessadoRepository;

    @Autowired
    private OutboxPublisher outboxPublisher;

    @Autowired
    private CountingPublicadorEventos publicadorEventos;

    @Autowired
    private TestValidadorSessaoCaixa validadorSessaoCaixa;

    @BeforeEach
    void setUp() {
        validadorSessaoCaixa.setValido(true);
        publicadorEventos.reset();
        pedidoRepository.deleteAll();
        outboxEventRepository.deleteAll();
        eventoProcessadoRepository.deleteAll();
    }

    @Test
    void criarPedidoComSessaoValidaDevePersistirPedidoRascunho() {
        Pedido pedido = pedidoService.criarPedido(
                "operador-1",
                "token-1",
                "cliente-1",
                "11999990000",
                List.of(new PedidoService.ItemVenda("produto-1", 1, new BigDecimal("10.00")))
        );

        assertThat(pedido.getStatus()).isEqualTo(StatusPedido.AGUARDANDO_PAGAMENTO);
        assertThat(pedidoRepository.findById(pedido.getId())).isPresent();
        assertThat(pedidoRepository.findById(pedido.getId()).orElseThrow().getItens()).hasSize(1);
    }

    @Test
    void criarPedidoComSessaoInvalidaNaoPersistePedido() {
        validadorSessaoCaixa.setValido(false);

        assertThatThrownBy(() -> pedidoService.criarPedido(
                "operador-1",
                "token-invalido",
                "cliente-1",
                "11999990000",
                List.of(new PedidoService.ItemVenda("produto-1", 1, new BigDecimal("10.00")))
        )).isInstanceOf(SessaoCaixaInvalidaException.class);

        assertThat(pedidoRepository.findAll()).isEmpty();
    }

    @Test
    void confirmarPagamentoAprovadoCriaOutboxEventPublicadoFalse() {
        Pedido pedido = pedidoService.criarPedido(
                "operador-1",
                "token-1",
                "cliente-1",
                "11999990000",
                List.of(new PedidoService.ItemVenda("produto-1", 1, new BigDecimal("10.00")))
        );

        Pedido confirmado = pedidoService.confirmarPagamento(pedido.getId(), true, "corr-1");

        assertThat(confirmado.getStatus()).isEqualTo(StatusPedido.PAGO);
        assertThat(outboxEventRepository.findAll()).hasSize(1);
        OutboxEvent outboxEvent = outboxEventRepository.findAll().get(0);
        assertThat(outboxEvent.getTipoEvento()).isEqualTo(TipoEvento.PEDIDO_CRIADO);
        assertThat(outboxEvent.isPublicado()).isFalse();
        assertThat(outboxEvent.getPayloadJson()).contains("PedidoCriado");
        assertThat(outboxEvent.getPayloadJson()).contains(pedido.getId().toString());
    }

    @Test
    void confirmarPagamentoRecusadoCancelaPedidoSemOutbox() {
        Pedido pedido = pedidoService.criarPedido(
                "operador-1",
                "token-1",
                "cliente-1",
                "11999990000",
                List.of(new PedidoService.ItemVenda("produto-1", 1, new BigDecimal("10.00")))
        );

        Pedido cancelado = pedidoService.confirmarPagamento(pedido.getId(), false, "corr-2");

        assertThat(cancelado.getStatus()).isEqualTo(StatusPedido.CANCELADO);
        assertThat(cancelado.getMotivoCancelamento()).isEqualTo("PAGAMENTO_RECUSADO");
        assertThat(outboxEventRepository.findAll()).isEmpty();
    }

    @Test
    void outboxPublisherPublicaPendenciaEAtualizaPedidoParaProcessandoSemRepublicar() {
        Pedido pedido = pedidoService.criarPedido(
                "operador-1",
                "token-1",
                "cliente-1",
                "11999990000",
                List.of(new PedidoService.ItemVenda("produto-1", 1, new BigDecimal("10.00")))
        );
        pedidoService.confirmarPagamento(pedido.getId(), true, "corr-3");

        outboxPublisher.publicarPendentes();

        assertThat(publicadorEventos.getChamadas()).isEqualTo(1);
        OutboxEvent publicado = outboxEventRepository.findAll().get(0);
        assertThat(publicado.isPublicado()).isTrue();
        Pedido pedidoAtualizado = pedidoRepository.findById(pedido.getId()).orElseThrow();
        assertThat(pedidoAtualizado.getStatus()).isEqualTo(StatusPedido.PROCESSANDO);

        outboxPublisher.publicarPendentes();

        assertThat(publicadorEventos.getChamadas()).isEqualTo(1);
    }

    @Test
    void processarEstoqueInsuficienteAtualizaPedidoParaCancelado() {
        Pedido pedido = pedidoService.criarPedido(
                "operador-1",
                "token-1",
                "cliente-1",
                "11999990000",
                List.of(new PedidoService.ItemVenda("produto-1", 1, new BigDecimal("10.00")))
        );
        pedidoService.confirmarPagamento(pedido.getId(), true, "corr-4");
        pedidoService.marcarProcessandoAposPublicacao(pedido.getId().toString());

        EnvelopeEvento<EstoqueInsuficientePayload> evento = EnvelopeEvento.create(
                TipoEvento.ESTOQUE_INSUFICIENTE,
                pedido.getId().toString(),
                "corr-4",
                new EstoqueInsuficientePayload(pedido.getId().toString(), "FALTA_DE_ESTOQUE", List.of(new ItemPedidoDto("produto-1", 1)), List.of(new ItemPedidoDto("produto-1", 1)))
        );

        pedidoService.processarEstoqueInsuficiente(evento);

        Pedido pedidoCancelado = pedidoRepository.findById(pedido.getId()).orElseThrow();
        assertThat(pedidoCancelado.getStatus()).isEqualTo(StatusPedido.CANCELADO);
        assertThat(pedidoCancelado.getMotivoCancelamento()).isEqualTo("ESTOQUE_INSUFICIENTE");
    }

    @Test
    void processarEstoqueInsuficienteComMesmoEventIdEhIdempotente() {
        Pedido pedido = pedidoService.criarPedido(
                "operador-1",
                "token-1",
                "cliente-1",
                "11999990000",
                List.of(new PedidoService.ItemVenda("produto-1", 1, new BigDecimal("10.00")))
        );
        pedidoService.confirmarPagamento(pedido.getId(), true, "corr-5");
        pedidoService.marcarProcessandoAposPublicacao(pedido.getId().toString());

        EnvelopeEvento<EstoqueInsuficientePayload> evento = EnvelopeEvento.create(
                TipoEvento.ESTOQUE_INSUFICIENTE,
                pedido.getId().toString(),
                "corr-5",
                new EstoqueInsuficientePayload(pedido.getId().toString(), "FALTA_DE_ESTOQUE", List.of(new ItemPedidoDto("produto-1", 1)), List.of(new ItemPedidoDto("produto-1", 1)))
        );

        pedidoService.processarEstoqueInsuficiente(evento);
        pedidoService.processarEstoqueInsuficiente(evento);

        assertThat(eventoProcessadoRepository.findAll()).hasSize(1);
    }

    @Test
    void confirmarPagamentoNaoFazChamadasSincronasAOutrosServicos() {
        Pedido pedido = pedidoService.criarPedido(
                "operador-1",
                "token-1",
                "cliente-1",
                "11999990000",
                List.of(new PedidoService.ItemVenda("produto-1", 1, new BigDecimal("10.00")))
        );

        pedidoService.confirmarPagamento(pedido.getId(), true, "corr-6");

        assertThat(publicadorEventos.getChamadas()).isZero();
        assertThat(outboxEventRepository.findAll()).hasSize(1);
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

        @Bean(name = "validadorSessaoCaixa")
        @Primary
        TestValidadorSessaoCaixa testValidadorSessaoCaixa() {
            return new TestValidadorSessaoCaixa();
        }
    }

    static class TestValidadorSessaoCaixa implements ValidadorSessaoCaixa {
        private boolean valido = true;

        @Override
        public boolean vendaAtiva(String operadorId, String tokenSessaoCaixa) {
            return valido;
        }

        void setValido(boolean valido) {
            this.valido = valido;
        }
    }

    static class CountingPublicadorEventos extends PublicadorEventos {
        private int chamadas;

        @Override
        public void publicar(EnvelopeEvento<?> evento) {
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
