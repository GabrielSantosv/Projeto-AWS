package com.marketflow.supplier.service;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.marketflow.supplier.domain.EventoProcessado;
import com.marketflow.supplier.domain.Fornecedor;
import com.marketflow.supplier.domain.OrdemCompra;
import com.marketflow.supplier.event.EnvelopeEvento;
import com.marketflow.supplier.event.TipoEvento;
import com.marketflow.supplier.event.dto.EstoqueAtualizadoPayload;
import com.marketflow.supplier.event.dto.OrdemCompraGeradaPayload;
import com.marketflow.supplier.event.dto.EstoqueAtualizadoPayload.ItemEstoqueAtualizado;
import com.marketflow.supplier.event.publisher.PublicadorEventos;
import com.marketflow.supplier.repository.EventoProcessadoRepository;
import com.marketflow.supplier.repository.FornecedorRepository;
import com.marketflow.supplier.repository.OrdemCompraRepository;

@Service
public class SupplierService {

    private static final Logger log = LoggerFactory.getLogger(SupplierService.class);

    private final FornecedorRepository fornecedorRepository;
    private final OrdemCompraRepository ordemCompraRepository;
    private final EventoProcessadoRepository eventoProcessadoRepository;
    private final PublicadorEventos publicadorEventos;

    public SupplierService(
            FornecedorRepository fornecedorRepository,
            OrdemCompraRepository ordemCompraRepository,
            EventoProcessadoRepository eventoProcessadoRepository,
            PublicadorEventos publicadorEventos
    ) {
        this.fornecedorRepository = fornecedorRepository;
        this.ordemCompraRepository = ordemCompraRepository;
        this.eventoProcessadoRepository = eventoProcessadoRepository;
        this.publicadorEventos = publicadorEventos;
    }

    @Transactional
    public void processarEstoqueAtualizado(EnvelopeEvento<EstoqueAtualizadoPayload> evento) {
        if (eventoProcessadoRepository.existsById(evento.eventId())) {
            return;
        }

        EstoqueAtualizadoPayload payload = evento.payload();
        if (!payload.estoqueBaixo()) {
            registrarProcessado(evento);
            return;
        }

        payload.itens().stream()
                .filter(ItemEstoqueAtualizado::estoqueBaixo)
                .forEach(item -> gerarOrdemSeHouverFornecedor(evento, payload.pedidoId(), item));
        registrarProcessado(evento);
    }

    @Transactional
    public Fornecedor criarFornecedor(
            String produtoId,
            String nome,
            BigDecimal preco,
            int prazoDias,
            boolean ativo
    ) {
        return fornecedorRepository.save(new Fornecedor(produtoId, nome, preco, prazoDias, ativo));
    }

    @Transactional(readOnly = true)
    public List<Fornecedor> listarFornecedores() {
        return fornecedorRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Fornecedor buscarFornecedor(UUID id) {
        return fornecedorRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Fornecedor nao encontrado: " + id));
    }

    @Transactional
    public Fornecedor atualizarFornecedor(
            UUID id,
            String nome,
            BigDecimal preco,
            int prazoDias,
            boolean ativo
    ) {
        Fornecedor fornecedor = buscarFornecedor(id);
        fornecedor.atualizar(nome, preco, prazoDias, ativo);
        return fornecedorRepository.save(fornecedor);
    }

    @Transactional
    public void removerFornecedor(UUID id) {
        fornecedorRepository.delete(buscarFornecedor(id));
    }

    private void gerarOrdemSeHouverFornecedor(
            EnvelopeEvento<EstoqueAtualizadoPayload> evento,
            String pedidoId,
            ItemEstoqueAtualizado item
    ) {
        Fornecedor fornecedor = fornecedorRepository
                .findByProdutoIdAndAtivoTrueOrderByPrecoAsc(item.produtoId())
                .stream()
                .findFirst()
                .orElse(null);
        if (fornecedor == null) {
            log.error("Nenhum fornecedor ativo encontrado para produto {}", item.produtoId());
            return;
        }

        int quantidade = calcularQuantidadeReposicao(item);
        OrdemCompra ordem = ordemCompraRepository.save(new OrdemCompra(
                item.produtoId(),
                fornecedor.getId(),
                quantidade,
                fornecedor.getPreco()
        ));

        // O evento e persistido antes da simulacao externa, mantendo o desacoplamento.
        publicadorEventos.publicar(EnvelopeEvento.create(
                TipoEvento.ORDEM_COMPRA_GERADA,
                evento.sagaId(),
                evento.correlationId(),
                new OrdemCompraGeradaPayload(
                        ordem.getId(),
                        pedidoId,
                        ordem.getProdutoId(),
                        ordem.getFornecedorId(),
                        ordem.getQuantidade(),
                        ordem.getStatus().name()
                )
        ));

        simularApiExternaFornecedor(ordem);
        ordem.confirmar();
        ordemCompraRepository.save(ordem);
    }

    private int calcularQuantidadeReposicao(ItemEstoqueAtualizado item) {
        return Math.max(1, item.limiteEstoqueBaixo() - item.quantidadeDisponivel());
    }

    private void simularApiExternaFornecedor(OrdemCompra ordem) {
        log.info(
                "API externa simulada confirmou a ordem de compra {} do produto {}",
                ordem.getId(),
                ordem.getProdutoId()
        );
    }

    private void registrarProcessado(EnvelopeEvento<?> evento) {
        eventoProcessadoRepository.save(new EventoProcessado(evento.eventId(), evento.eventType()));
    }
}
