package com.marketflow.estoque.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.marketflow.estoque.domain.EventoProcessado;
import com.marketflow.estoque.domain.Produto;
import com.marketflow.estoque.domain.ReservaEstoque;
import com.marketflow.estoque.domain.StatusReserva;
import com.marketflow.estoque.event.EnvelopeEvento;
import com.marketflow.estoque.event.TipoEvento;
import com.marketflow.estoque.event.dto.EstoqueAtualizadoPayload;
import com.marketflow.estoque.event.dto.EstoqueInsuficientePayload;
import com.marketflow.estoque.event.dto.ItemPedido;
import com.marketflow.estoque.event.dto.PedidoCriadoPayload;
import com.marketflow.estoque.event.publisher.PublicadorEventos;
import com.marketflow.estoque.repository.EventoProcessadoRepository;
import com.marketflow.estoque.repository.ProdutoRepository;
import com.marketflow.estoque.repository.ReservaEstoqueRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class EstoqueServiceTest {

    @Mock
    private ProdutoRepository produtoRepository;

    @Mock
    private ReservaEstoqueRepository reservaRepository;

    @Mock
    private EventoProcessadoRepository eventoProcessadoRepository;

    @Mock
    private PublicadorEventos publicadorEventos;

    private EstoqueService estoqueService;

    @BeforeEach
    void setUp() {
        estoqueService = new EstoqueService(
                produtoRepository,
                reservaRepository,
                eventoProcessadoRepository,
                publicadorEventos
        );
        lenient().when(reservaRepository.save(any(ReservaEstoque.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void reservaComSucessoParaUmUnicoItem() {
        Produto produto = produto("PROD-001", 10, 2);
        when(eventoProcessadoRepository.existsById("evt-1")).thenReturn(false);
        when(produtoRepository.findByIdForUpdate("PROD-001")).thenReturn(Optional.of(produto));

        estoqueService.processarPedidoCriado(evento("evt-1", List.of(new ItemPedido("PROD-001", 3))));

        assertThat(produto.getQuantidadeReservada()).isEqualTo(3);
        assertThat(produto.getQuantidadeDisponivel()).isEqualTo(7);

        ArgumentCaptor<ReservaEstoque> reservaCaptor = ArgumentCaptor.forClass(ReservaEstoque.class);
        verify(reservaRepository).save(reservaCaptor.capture());
        assertThat(reservaCaptor.getValue().getStatus()).isEqualTo(StatusReserva.RESERVADA);
        assertThat(reservaCaptor.getValue().getQuantidade()).isEqualTo(3);

        EnvelopeEvento<?> publicado = capturarEventoPublicado();
        assertThat(publicado.eventType()).isEqualTo(TipoEvento.ESTOQUE_ATUALIZADO);
        EstoqueAtualizadoPayload payload = (EstoqueAtualizadoPayload) publicado.payload();
        assertThat(payload.pedidoId()).isEqualTo("PED-001");
        assertThat(payload.estoqueBaixo()).isFalse();
        assertThat(payload.itens()).singleElement()
                .satisfies(item -> {
                    assertThat(item.produtoId()).isEqualTo("PROD-001");
                    assertThat(item.quantidadeReservada()).isEqualTo(3);
                    assertThat(item.quantidadeDisponivel()).isEqualTo(7);
                    assertThat(item.estoqueBaixo()).isFalse();
                });

        verify(eventoProcessadoRepository).save(any(EventoProcessado.class));
    }

    @Test
    void reservaComSucessoParaMultiplosItensDoMesmoPedido() {
        Produto produtoUm = produto("PROD-001", 10, 2);
        Produto produtoDois = produto("PROD-002", 6, 2);
        when(eventoProcessadoRepository.existsById("evt-2")).thenReturn(false);
        when(produtoRepository.findByIdForUpdate("PROD-001")).thenReturn(Optional.of(produtoUm));
        when(produtoRepository.findByIdForUpdate("PROD-002")).thenReturn(Optional.of(produtoDois));

        estoqueService.processarPedidoCriado(evento("evt-2", List.of(
                new ItemPedido("PROD-001", 4),
                new ItemPedido("PROD-002", 2)
        )));

        assertThat(produtoUm.getQuantidadeReservada()).isEqualTo(4);
        assertThat(produtoDois.getQuantidadeReservada()).isEqualTo(2);
        verify(reservaRepository, times(2)).save(any(ReservaEstoque.class));

        EnvelopeEvento<?> publicado = capturarEventoPublicado();
        assertThat(publicado.eventType()).isEqualTo(TipoEvento.ESTOQUE_ATUALIZADO);
        EstoqueAtualizadoPayload payload = (EstoqueAtualizadoPayload) publicado.payload();
        assertThat(payload.itens()).extracting(EstoqueAtualizadoPayload.ItemEstoqueAtualizado::produtoId)
                .containsExactly("PROD-001", "PROD-002");
    }

    @Test
    void falhaPorEstoqueInsuficienteReverteReservasParciaisEPublicaCompensacao() {
        Produto produtoReservadoAntesDaFalha = produto("PROD-001", 10, 2);
        Produto produtoSemEstoque = produto("PROD-002", 1, 2);
        when(eventoProcessadoRepository.existsById("evt-3")).thenReturn(false);
        when(produtoRepository.findByIdForUpdate("PROD-001")).thenReturn(Optional.of(produtoReservadoAntesDaFalha));
        when(produtoRepository.findByIdForUpdate("PROD-002")).thenReturn(Optional.of(produtoSemEstoque));

        estoqueService.processarPedidoCriado(evento("evt-3", List.of(
                new ItemPedido("PROD-001", 4),
                new ItemPedido("PROD-002", 2)
        )));

        assertThat(produtoReservadoAntesDaFalha.getQuantidadeReservada()).isZero();
        assertThat(produtoReservadoAntesDaFalha.getQuantidadeDisponivel()).isEqualTo(10);
        assertThat(produtoSemEstoque.getQuantidadeReservada()).isZero();

        ArgumentCaptor<ReservaEstoque> reservaCaptor = ArgumentCaptor.forClass(ReservaEstoque.class);
        verify(reservaRepository).save(reservaCaptor.capture());
        assertThat(reservaCaptor.getValue().getStatus()).isEqualTo(StatusReserva.CANCELADA);

        EnvelopeEvento<?> publicado = capturarEventoPublicado();
        assertThat(publicado.eventType()).isEqualTo(TipoEvento.ESTOQUE_INSUFICIENTE);
        EstoqueInsuficientePayload payload = (EstoqueInsuficientePayload) publicado.payload();
        assertThat(payload.pedidoId()).isEqualTo("PED-001");
        assertThat(payload.motivo()).contains("Estoque insuficiente");
        assertThat(payload.itensIndisponiveis()).containsExactly(new ItemPedido("PROD-002", 2));
    }

    @Test
    void produtoInexistentePublicaEstoqueInsuficienteSemReserva() {
        when(eventoProcessadoRepository.existsById("evt-4")).thenReturn(false);
        when(produtoRepository.findByIdForUpdate("PROD-404")).thenReturn(Optional.empty());

        estoqueService.processarPedidoCriado(evento("evt-4", List.of(new ItemPedido("PROD-404", 1))));

        verify(reservaRepository, never()).save(any(ReservaEstoque.class));

        EnvelopeEvento<?> publicado = capturarEventoPublicado();
        assertThat(publicado.eventType()).isEqualTo(TipoEvento.ESTOQUE_INSUFICIENTE);
        EstoqueInsuficientePayload payload = (EstoqueInsuficientePayload) publicado.payload();
        assertThat(payload.motivo()).contains("Produto nao encontrado");
        assertThat(payload.itensIndisponiveis()).containsExactly(new ItemPedido("PROD-404", 1));
    }

    @Test
    void eventoRepetidoNaoAlteraEstoqueENaoPublicaOutroEvento() {
        when(eventoProcessadoRepository.existsById("evt-dup")).thenReturn(true);

        estoqueService.processarPedidoCriado(evento("evt-dup", List.of(new ItemPedido("PROD-001", 3))));

        verifyNoInteractions(produtoRepository, reservaRepository, publicadorEventos);
        verify(eventoProcessadoRepository, never()).save(any(EventoProcessado.class));
    }

    @Test
    void ordenaProdutosAntesDeAdquirirLocks() {
        Produto produtoUm = produto("PROD-001", 10, 2);
        Produto produtoDois = produto("PROD-002", 10, 2);
        when(eventoProcessadoRepository.existsById("evt-order")).thenReturn(false);
        when(produtoRepository.findByIdForUpdate("PROD-001")).thenReturn(Optional.of(produtoUm));
        when(produtoRepository.findByIdForUpdate("PROD-002")).thenReturn(Optional.of(produtoDois));

        estoqueService.processarPedidoCriado(evento("evt-order", List.of(
                new ItemPedido("PROD-002", 1),
                new ItemPedido("PROD-001", 1)
        )));

        InOrder inOrder = inOrder(produtoRepository);
        inOrder.verify(produtoRepository).findByIdForUpdate("PROD-001");
        inOrder.verify(produtoRepository).findByIdForUpdate("PROD-002");
    }

    private Produto produto(String produtoId, int quantidadeDisponivelTotal, int limiteEstoqueBaixo) {
        return new Produto(
                produtoId,
                "Produto " + produtoId,
                BigDecimal.valueOf(10),
                quantidadeDisponivelTotal,
                limiteEstoqueBaixo
        );
    }

    private EnvelopeEvento<PedidoCriadoPayload> evento(String eventId, List<ItemPedido> itens) {
        return new EnvelopeEvento<>(
                eventId,
                TipoEvento.PEDIDO_CRIADO,
                "PED-001",
                "CORR-001",
                Instant.parse("2026-07-23T00:00:00Z"),
                1,
                new PedidoCriadoPayload("PED-001", "CLI-001", "+5511999999999", itens)
        );
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private EnvelopeEvento<?> capturarEventoPublicado() {
        ArgumentCaptor<EnvelopeEvento> eventoCaptor = ArgumentCaptor.forClass(EnvelopeEvento.class);
        verify(publicadorEventos).publicar(eventoCaptor.capture());
        return eventoCaptor.getValue();
    }
}
