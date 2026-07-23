package com.marketflow.estoque.service;

import com.marketflow.estoque.domain.EventoProcessado;
import com.marketflow.estoque.domain.Produto;
import com.marketflow.estoque.domain.ReservaEstoque;
import com.marketflow.estoque.event.EnvelopeEvento;
import com.marketflow.estoque.event.TipoEvento;
import com.marketflow.estoque.event.dto.EstoqueAtualizadoPayload;
import com.marketflow.estoque.event.dto.EstoqueAtualizadoPayload.ItemEstoqueAtualizado;
import com.marketflow.estoque.event.dto.EstoqueInsuficientePayload;
import com.marketflow.estoque.event.dto.ItemPedido;
import com.marketflow.estoque.event.dto.PedidoCriadoPayload;
import com.marketflow.estoque.event.publisher.PublicadorEventos;
import com.marketflow.estoque.exception.ProdutoNaoEncontradoException;
import com.marketflow.estoque.repository.EventoProcessadoRepository;
import com.marketflow.estoque.repository.ProdutoRepository;
import com.marketflow.estoque.repository.ReservaEstoqueRepository;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EstoqueService {

    private final ProdutoRepository produtoRepository;
    private final ReservaEstoqueRepository reservaRepository;
    private final EventoProcessadoRepository eventoProcessadoRepository;
    private final PublicadorEventos publicadorEventos;

    public EstoqueService(
            ProdutoRepository produtoRepository,
            ReservaEstoqueRepository reservaRepository,
            EventoProcessadoRepository eventoProcessadoRepository,
            PublicadorEventos publicadorEventos
    ) {
        this.produtoRepository = produtoRepository;
        this.reservaRepository = reservaRepository;
        this.eventoProcessadoRepository = eventoProcessadoRepository;
        this.publicadorEventos = publicadorEventos;
    }

    @Transactional
    public Produto criarProduto(
            String produtoId,
            String nome,
            BigDecimal precoUnitario,
            int quantidadeDisponivelTotal,
            int limiteEstoqueBaixo
    ) {
        if (produtoRepository.existsById(produtoId)) {
            throw new IllegalArgumentException("Produto ja existe: " + produtoId);
        }
        return produtoRepository.save(new Produto(
                produtoId,
                nome,
                precoUnitario,
                quantidadeDisponivelTotal,
                limiteEstoqueBaixo
        ));
    }

    @Transactional(readOnly = true)
    public Produto buscarProduto(String produtoId) {
        return produtoRepository.findById(produtoId)
                .orElseThrow(() -> new ProdutoNaoEncontradoException(produtoId));
    }

    @Transactional(readOnly = true)
    public List<Produto> listarProdutos() {
        return produtoRepository.findAll();
    }

    @Transactional
    public Produto ajustarEstoque(String produtoId, int delta) {
        Produto produto = produtoRepository.findByIdForUpdate(produtoId)
                .orElseThrow(() -> new ProdutoNaoEncontradoException(produtoId));
        produto.ajustarEstoque(delta);
        return produto;
    }

    @Transactional
    public void processarPedidoCriado(EnvelopeEvento<PedidoCriadoPayload> evento) {
        if (eventoProcessadoRepository.existsById(evento.eventId())) {
            return;
        }

        PedidoCriadoPayload payload = evento.payload();
        String pedidoId = normalizarPedidoId(payload, evento);
        List<ItemPedido> itensOriginais = payload.itens();
        List<ItemPedido> itensOrdenados = ordenarParaLocks(itensOriginais);
        List<ReservaEstoque> reservasCriadas = new ArrayList<>();

        FalhaReserva falha = reservarItens(pedidoId, itensOrdenados, reservasCriadas);

        if (falha != null) {
            cancelarReservasCriadas(reservasCriadas);
            publicarEstoqueInsuficiente(evento, pedidoId, itensOriginais, falha);
        } else {
            publicarEstoqueAtualizado(evento, pedidoId, reservasCriadas);
        }

        eventoProcessadoRepository.save(new EventoProcessado(evento.eventId(), evento.eventType()));
    }

    private FalhaReserva reservarItens(
            String pedidoId,
            List<ItemPedido> itensOrdenados,
            List<ReservaEstoque> reservasCriadas
    ) {
        for (ItemPedido item : itensOrdenados) {
            if (item.quantidade() <= 0) {
                return new FalhaReserva(
                        "Quantidade invalida para produto: " + item.produtoId(),
                        item
                );
            }

            Produto produto = produtoRepository.findByIdForUpdate(item.produtoId()).orElse(null);
            if (produto == null) {
                return new FalhaReserva(
                        "Produto nao encontrado: " + item.produtoId(),
                        item
                );
            }

            if (!produto.temEstoqueSuficiente(item.quantidade())) {
                return new FalhaReserva(
                        "Estoque insuficiente para produto: " + item.produtoId(),
                        item
                );
            }

            produto.reservar(item.quantidade());
            reservasCriadas.add(reservaRepository.save(new ReservaEstoque(
                    pedidoId,
                    produto,
                    item.quantidade()
            )));
        }
        return null;
    }

    private void cancelarReservasCriadas(List<ReservaEstoque> reservasCriadas) {
        for (ReservaEstoque reserva : reservasCriadas) {
            reserva.getProduto().liberarReserva(reserva.getQuantidade());
            reserva.cancelar();
        }
    }

    private void publicarEstoqueAtualizado(
            EnvelopeEvento<PedidoCriadoPayload> evento,
            String pedidoId,
            List<ReservaEstoque> reservasCriadas
    ) {
        List<ItemEstoqueAtualizado> itensAtualizados = reservasCriadas.stream()
                .map(reserva -> new ItemEstoqueAtualizado(
                        reserva.getProduto().getProdutoId(),
                        reserva.getQuantidade(),
                        reserva.getProduto().getQuantidadeDisponivel(),
                        reserva.getProduto().getLimiteEstoqueBaixo(),
                        reserva.getProduto().isEstoqueBaixo()
                ))
                .toList();

        boolean estoqueBaixo = itensAtualizados.stream().anyMatch(ItemEstoqueAtualizado::estoqueBaixo);
        publicadorEventos.publicar(EnvelopeEvento.create(
                TipoEvento.ESTOQUE_ATUALIZADO,
                evento.sagaId(),
                evento.correlationId(),
                new EstoqueAtualizadoPayload(pedidoId, itensAtualizados, estoqueBaixo)
        ));
    }

    private void publicarEstoqueInsuficiente(
            EnvelopeEvento<PedidoCriadoPayload> evento,
            String pedidoId,
            List<ItemPedido> itensOriginais,
            FalhaReserva falha
    ) {
        publicadorEventos.publicar(EnvelopeEvento.create(
                TipoEvento.ESTOQUE_INSUFICIENTE,
                evento.sagaId(),
                evento.correlationId(),
                new EstoqueInsuficientePayload(
                        pedidoId,
                        falha.motivo(),
                        itensOriginais,
                        List.of(falha.item())
                )
        ));
    }

    private String normalizarPedidoId(PedidoCriadoPayload payload, EnvelopeEvento<PedidoCriadoPayload> evento) {
        if (payload.pedidoId() != null && !payload.pedidoId().isBlank()) {
            return payload.pedidoId();
        }
        return evento.sagaId();
    }

    private List<ItemPedido> ordenarParaLocks(List<ItemPedido> itens) {
        return itens.stream()
                .sorted(Comparator.comparing(ItemPedido::produtoId))
                .toList();
    }

    private record FalhaReserva(String motivo, ItemPedido item) {
    }
}
