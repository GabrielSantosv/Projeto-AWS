package com.marketflow.supplier;

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

import com.marketflow.supplier.domain.EventoProcessado;
import com.marketflow.supplier.domain.Fornecedor;
import com.marketflow.supplier.domain.OrdemCompra;
import com.marketflow.supplier.domain.OutboxEvent;
import com.marketflow.supplier.domain.StatusOrdemCompra;
import com.marketflow.supplier.event.EnvelopeEvento;
import com.marketflow.supplier.event.TipoEvento;
import com.marketflow.supplier.event.dto.EstoqueAtualizadoPayload;
import com.marketflow.supplier.event.dto.EstoqueAtualizadoPayload.ItemEstoqueAtualizado;
import com.marketflow.supplier.repository.EventoProcessadoRepository;
import com.marketflow.supplier.repository.FornecedorRepository;
import com.marketflow.supplier.repository.OrdemCompraRepository;
import com.marketflow.supplier.repository.OutboxEventRepository;
import com.marketflow.supplier.service.SupplierService;

import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sns.model.PublishRequest;

@SpringBootTest
@ActiveProfiles("test")
class SupplierServiceIntegrationTest {

    @Autowired
    private SupplierService supplierService;

    @Autowired
    private FornecedorRepository fornecedorRepository;

    @Autowired
    private OrdemCompraRepository ordemCompraRepository;

    @Autowired
    private EventoProcessadoRepository eventoProcessadoRepository;

    @Autowired
    private OutboxEventRepository outboxEventRepository;

    @MockBean
    private SnsClient snsClient;

    @BeforeEach
    void setUp() {
        outboxEventRepository.deleteAll();
        ordemCompraRepository.deleteAll();
        eventoProcessadoRepository.deleteAll();
        fornecedorRepository.deleteAll();
    }

    @Test
    void estoqueBaixoGeraOrdemConfirmaEPublicaEvento() {
        Fornecedor fornecedor = fornecedorRepository.save(
                new Fornecedor("produto-1", "Fornecedor A", new BigDecimal("12.50"), 3, true)
        );

        supplierService.processarEstoqueAtualizado(estoqueAtualizado(
                "evt-1", "pedido-1", true, item("produto-1", 2, 3, 5, true)
        ));

        OrdemCompra ordem = ordemCompraRepository.findAll().stream().findFirst().orElseThrow();
        assertThat(ordem.getFornecedorId()).isEqualTo(fornecedor.getId());
        assertThat(ordem.getQuantidade()).isEqualTo(2);
        assertThat(ordem.getStatus()).isEqualTo(StatusOrdemCompra.CONFIRMADA);
        OutboxEvent outbox = outboxEventRepository.findAll().stream().findFirst().orElseThrow();
        assertThat(outbox.getEventType()).isEqualTo(TipoEvento.ORDEM_COMPRA_GERADA);
        assertThat(outbox.isPublicado()).isFalse();
    }

    @Test
    void estoqueNaoBaixoApenasMarcaEventoProcessado() {
        supplierService.processarEstoqueAtualizado(estoqueAtualizado(
                "evt-2", "pedido-2", false, item("produto-2", 2, 10, 5, false)
        ));

        assertThat(ordemCompraRepository.findAll()).isEmpty();
        assertThat(outboxEventRepository.findAll()).isEmpty();
        assertThat(eventoProcessadoRepository.findAll()).hasSize(1);
    }

    @Test
    void eventoDuplicadoEhIdempotente() {
        fornecedorRepository.save(new Fornecedor("produto-3", "Fornecedor B", new BigDecimal("8.00"), 2, true));
        EnvelopeEvento<EstoqueAtualizadoPayload> evento = estoqueAtualizado(
                "evt-duplicado", "pedido-3", true, item("produto-3", 1, 1, 5, true)
        );

        supplierService.processarEstoqueAtualizado(evento);
        supplierService.processarEstoqueAtualizado(evento);

        assertThat(ordemCompraRepository.findAll()).hasSize(1);
        assertThat(outboxEventRepository.findAll()).hasSize(1);
        assertThat(eventoProcessadoRepository.findAll()).hasSize(1);
    }

    @Test
    void produtoSemFornecedorNaoPublicaSucessoENaoDerrubaListener() {
        supplierService.processarEstoqueAtualizado(estoqueAtualizado(
                "evt-sem-fornecedor", "pedido-4", true, item("produto-404", 1, 1, 5, true)
        ));

        assertThat(ordemCompraRepository.findAll()).isEmpty();
        assertThat(outboxEventRepository.findAll()).isEmpty();
        assertThat(eventoProcessadoRepository.findAll()).hasSize(1);
    }

    private EnvelopeEvento<EstoqueAtualizadoPayload> estoqueAtualizado(
            String eventId,
            String pedidoId,
            boolean estoqueBaixo,
            ItemEstoqueAtualizado... itens
    ) {
        EnvelopeEvento<EstoqueAtualizadoPayload> criado = EnvelopeEvento.create(
                TipoEvento.ESTOQUE_ATUALIZADO,
                pedidoId,
                "corr-" + pedidoId,
                new EstoqueAtualizadoPayload(pedidoId, List.of(itens), estoqueBaixo)
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

    private ItemEstoqueAtualizado item(
            String produtoId,
            int quantidadeReservada,
            int quantidadeDisponivel,
            int limite,
            boolean estoqueBaixo
    ) {
        return new ItemEstoqueAtualizado(
                produtoId,
                quantidadeReservada,
                quantidadeDisponivel,
                limite,
                estoqueBaixo
        );
    }
}
