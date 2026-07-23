package com.marketflow.pedido.controller;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.marketflow.pedido.domain.Pedido;
import com.marketflow.pedido.service.PedidoService;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

@RestController
@RequestMapping("/pedidos")
public class PedidoController {

    private final PedidoService pedidoService;

    public PedidoController(PedidoService pedidoService) {
        this.pedidoService = pedidoService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Pedido criarPedido(@RequestBody @Valid PedidoRequest request) {
        return pedidoService.criarPedido(
                request.operadorId(),
                request.tokenSessaoCaixa(),
                request.clienteId(),
                request.telefoneCliente(),
                request.itens().stream().map(PedidoController::toItemVenda).toList()
        );
    }

    @PostMapping("/{id}/confirmar-pagamento")
    public Pedido confirmarPagamento(@PathVariable UUID id, @RequestBody @Valid ConfirmarPagamentoRequest request) {
        return pedidoService.confirmarPagamento(id, request.pagamentoAprovado(), request.correlationId());
    }

    @GetMapping("/{id}")
    public Pedido buscarPedido(@PathVariable UUID id) {
        return pedidoService.buscarPedido(id);
    }

    private static PedidoService.ItemVenda toItemVenda(ItemRequest item) {
        return new PedidoService.ItemVenda(item.produtoId(), item.quantidade(), item.precoUnitario());
    }

    public record PedidoRequest(
            @NotBlank String operadorId,
            @NotBlank String tokenSessaoCaixa,
            String clienteId,
            String telefoneCliente,
            @NotEmpty List<@Valid ItemRequest> itens
    ) {
    }

    public record ItemRequest(
            @NotBlank String produtoId,
            @Positive int quantidade,
            @NotNull BigDecimal precoUnitario
    ) {
    }

    public record ConfirmarPagamentoRequest(
            boolean pagamentoAprovado,
            String correlationId
    ) {
    }
}
