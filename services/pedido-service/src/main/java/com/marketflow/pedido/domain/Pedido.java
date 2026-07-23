package com.marketflow.pedido.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

@Entity
@Table(name = "pedidos")
public class Pedido {

    @Id
    private UUID id;

    @Column(name = "operador_id", nullable = false, length = 120)
    private String operadorId;

    @Column(name = "cliente_id", length = 120)
    private String clienteId;

    @Column(name = "telefone_cliente", length = 40)
    private String telefoneCliente;

    @Column(name = "valor_total", nullable = false, precision = 12, scale = 2)
    private BigDecimal valorTotal;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private StatusPedido status;

    @Column(name = "motivo_cancelamento", length = 120)
    private String motivoCancelamento;

    @Column(name = "criado_em", nullable = false)
    private Instant criadoEm;

    @Column(name = "atualizado_em", nullable = false)
    private Instant atualizadoEm;

    @OneToMany(mappedBy = "pedido", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private List<ItemPedido> itens = new ArrayList<>();

    protected Pedido() {
    }

    public Pedido(String operadorId, String clienteId, String telefoneCliente) {
        if (operadorId == null || operadorId.isBlank()) {
            throw new IllegalArgumentException("operadorId nao pode ser vazio");
        }
        this.id = UUID.randomUUID();
        this.operadorId = operadorId;
        this.clienteId = clienteId;
        this.telefoneCliente = telefoneCliente;
        this.valorTotal = BigDecimal.ZERO;
        this.status = StatusPedido.RASCUNHO;
        this.criadoEm = Instant.now();
        this.atualizadoEm = this.criadoEm;
    }

    public UUID getId() {
        return id;
    }

    public String getOperadorId() {
        return operadorId;
    }

    public String getClienteId() {
        return clienteId;
    }

    public String getTelefoneCliente() {
        return telefoneCliente;
    }

    public BigDecimal getValorTotal() {
        return valorTotal;
    }

    public StatusPedido getStatus() {
        return status;
    }

    public String getMotivoCancelamento() {
        return motivoCancelamento;
    }

    public List<ItemPedido> getItens() {
        return Collections.unmodifiableList(itens);
    }

    public void adicionarItem(String produtoId, int quantidade, BigDecimal precoUnitario) {
        ItemPedido item = new ItemPedido(this, produtoId, quantidade, precoUnitario);
        itens.add(item);
        recalcularValorTotal();
        tocar();
    }

    public void aguardarPagamento() {
        if (status != StatusPedido.RASCUNHO) {
            throw new IllegalStateException("Pedido precisa estar RASCUNHO para aguardar pagamento");
        }
        status = StatusPedido.AGUARDANDO_PAGAMENTO;
        tocar();
    }

    public void confirmarPagamentoAprovado() {
        exigirStatusParaPagamento();
        status = StatusPedido.PAGO;
        motivoCancelamento = null;
        tocar();
    }

    public void cancelarPorPagamentoRecusado() {
        exigirStatusParaPagamento();
        status = StatusPedido.CANCELADO;
        motivoCancelamento = "PAGAMENTO_RECUSADO";
        tocar();
    }

    public void marcarProcessando() {
        if (status == StatusPedido.PROCESSANDO) {
            return;
        }
        if (status != StatusPedido.PAGO) {
            throw new IllegalStateException("Pedido precisa estar PAGO para virar PROCESSANDO");
        }
        status = StatusPedido.PROCESSANDO;
        tocar();
    }

    public void cancelarPorEstoqueInsuficiente() {
        if (status == StatusPedido.CANCELADO) {
            return;
        }
        if (status != StatusPedido.PROCESSANDO) {
            return;
        }
        status = StatusPedido.CANCELADO;
        motivoCancelamento = "ESTOQUE_INSUFICIENTE";
        tocar();
    }

    // TODO: quando expedicao-service publicar SeparacaoPedidoIniciado, avaliar transicao para COMPLETO.

    private void exigirStatusParaPagamento() {
        if (status != StatusPedido.RASCUNHO && status != StatusPedido.AGUARDANDO_PAGAMENTO) {
            throw new IllegalStateException("Pedido nao esta aguardando pagamento");
        }
    }

    private void recalcularValorTotal() {
        valorTotal = itens.stream()
                .map(ItemPedido::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private void tocar() {
        atualizadoEm = Instant.now();
    }
}
