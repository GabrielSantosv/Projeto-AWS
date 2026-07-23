package com.marketflow.pedido.service;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketflow.pedido.domain.EventoProcessado;
import com.marketflow.pedido.domain.ItemPedido;
import com.marketflow.pedido.domain.OutboxEvent;
import com.marketflow.pedido.domain.Pedido;
import com.marketflow.pedido.event.EnvelopeEvento;
import com.marketflow.pedido.event.TipoEvento;
import com.marketflow.pedido.event.dto.EstoqueInsuficientePayload;
import com.marketflow.pedido.event.dto.ItemPedidoDto;
import com.marketflow.pedido.event.dto.PedidoCriadoPayload;
import com.marketflow.pedido.exception.PedidoNaoEncontradoException;
import com.marketflow.pedido.exception.SessaoCaixaInvalidaException;
import com.marketflow.pedido.repository.EventoProcessadoRepository;
import com.marketflow.pedido.repository.OutboxEventRepository;
import com.marketflow.pedido.repository.PedidoRepository;

@Service
public class PedidoService {

    private final PedidoRepository pedidoRepository;
    private final OutboxEventRepository outboxEventRepository;
    private final EventoProcessadoRepository eventoProcessadoRepository;
    private final ObjectMapper objectMapper;
    private final ValidadorSessaoCaixa validadorSessaoCaixa;

    public PedidoService(
            PedidoRepository pedidoRepository,
            OutboxEventRepository outboxEventRepository,
            EventoProcessadoRepository eventoProcessadoRepository,
            ObjectMapper objectMapper,
            ValidadorSessaoCaixa validadorSessaoCaixa
    ) {
        this.pedidoRepository = pedidoRepository;
        this.outboxEventRepository = outboxEventRepository;
        this.eventoProcessadoRepository = eventoProcessadoRepository;
        this.objectMapper = objectMapper;
        this.validadorSessaoCaixa = validadorSessaoCaixa;
    }

    @Transactional
    public Pedido criarPedido(
            String operadorId,
            String tokenSessaoCaixa,
            String clienteId,
            String telefoneCliente,
            List<ItemVenda> itens
    ) {
        if (!validadorSessaoCaixa.vendaAtiva(operadorId, tokenSessaoCaixa)) {
            throw new SessaoCaixaInvalidaException();
        }
        if (itens == null || itens.isEmpty()) {
            throw new IllegalArgumentException("Pedido precisa ter pelo menos um item");
        }

        Pedido pedido = new Pedido(operadorId, clienteId, telefoneCliente);
        itens.forEach(item -> pedido.adicionarItem(item.produtoId(), item.quantidade(), item.precoUnitario()));
        pedido.aguardarPagamento();
        return pedidoRepository.save(pedido);
    }

    @Transactional(readOnly = true)
    public Pedido buscarPedido(UUID pedidoId) {
        return pedidoRepository.findById(pedidoId)
                .orElseThrow(() -> new PedidoNaoEncontradoException(pedidoId));
    }

    @Transactional(readOnly = true)
    public List<Pedido> listarPedidos() {
        return pedidoRepository.findAll();
    }

    @Transactional
    public Pedido confirmarPagamento(UUID pedidoId, boolean pagamentoAprovado, String correlationId) {
        Pedido pedido = pedidoRepository.findByIdForUpdate(pedidoId)
                .orElseThrow(() -> new PedidoNaoEncontradoException(pedidoId));

        if (!pagamentoAprovado) {
            pedido.cancelarPorPagamentoRecusado();
            return pedido;
        }

        pedido.confirmarPagamentoAprovado();
        EnvelopeEvento<PedidoCriadoPayload> evento = EnvelopeEvento.create(
                TipoEvento.PEDIDO_CRIADO,
                pedido.getId().toString(),
                normalizarCorrelationId(correlationId),
                toPedidoCriadoPayload(pedido)
        );
        outboxEventRepository.save(toOutboxEvent(evento));
        return pedido;
    }

    @Transactional(propagation = Propagation.REQUIRED)
    public void marcarProcessandoAposPublicacao(String pedidoId) {
        UUID id = UUID.fromString(pedidoId);
        Pedido pedido = pedidoRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new PedidoNaoEncontradoException(id));
        pedido.marcarProcessando();
    }

    @Transactional
    public void marcarProcessandoAposPublicacaoSemTransacao(String pedidoId) {
        UUID id = UUID.fromString(pedidoId);
        Pedido pedido = pedidoRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new PedidoNaoEncontradoException(id));
        pedido.marcarProcessando();
    }

    @Transactional
    public void processarEstoqueInsuficiente(EnvelopeEvento<EstoqueInsuficientePayload> evento) {
        if (eventoProcessadoRepository.existsById(evento.eventId())) {
            return;
        }

        UUID pedidoId = UUID.fromString(normalizarPedidoId(evento));
        Pedido pedido = pedidoRepository.findByIdForUpdate(pedidoId)
                .orElseThrow(() -> new PedidoNaoEncontradoException(pedidoId));
        pedido.cancelarPorEstoqueInsuficiente();
        eventoProcessadoRepository.save(new EventoProcessado(evento.eventId(), evento.eventType()));
    }

    private PedidoCriadoPayload toPedidoCriadoPayload(Pedido pedido) {
        return new PedidoCriadoPayload(
                pedido.getId().toString(),
                pedido.getClienteId(),
                pedido.getTelefoneCliente(),
                pedido.getOperadorId(),
                pedido.getValorTotal(),
                pedido.getItens().stream().map(this::toItemPedidoDto).toList()
        );
    }

    private ItemPedidoDto toItemPedidoDto(ItemPedido item) {
        return new ItemPedidoDto(item.getProdutoId(), item.getQuantidade());
    }

    private OutboxEvent toOutboxEvent(EnvelopeEvento<PedidoCriadoPayload> evento) {
        try {
            return new OutboxEvent(
                    evento.eventId(),
                    evento.eventType(),
                    evento.sagaId(),
                    objectMapper.writeValueAsString(evento)
            );
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Nao foi possivel serializar evento PedidoCriado", exception);
        }
    }

    private String normalizarCorrelationId(String correlationId) {
        if (correlationId == null || correlationId.isBlank()) {
            return UUID.randomUUID().toString();
        }
        return correlationId;
    }

    private String normalizarPedidoId(EnvelopeEvento<EstoqueInsuficientePayload> evento) {
        if (evento.payload().pedidoId() != null && !evento.payload().pedidoId().isBlank()) {
            return evento.payload().pedidoId();
        }
        return evento.sagaId();
    }

    public record ItemVenda(String produtoId, int quantidade, BigDecimal precoUnitario) {
    }
}
