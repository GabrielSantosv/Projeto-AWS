package com.marketflow.estoque.controller;

import com.marketflow.estoque.controller.ProdutoDtos.AjustarEstoqueRequest;
import com.marketflow.estoque.controller.ProdutoDtos.CriarProdutoRequest;
import com.marketflow.estoque.controller.ProdutoDtos.ProdutoResponse;
import com.marketflow.estoque.domain.Produto;
import com.marketflow.estoque.service.EstoqueService;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/produtos")
public class ProdutoController {

    private final EstoqueService estoqueService;

    public ProdutoController(EstoqueService estoqueService) {
        this.estoqueService = estoqueService;
    }

    @PostMapping
    ResponseEntity<ProdutoResponse> criarProduto(@Valid @RequestBody CriarProdutoRequest request) {
        Produto produto = estoqueService.criarProduto(
                request.produtoId(),
                request.nome(),
                request.precoUnitario(),
                request.quantidadeDisponivelTotal(),
                request.limiteEstoqueBaixo()
        );
        return ResponseEntity.created(URI.create("/produtos/" + produto.getProdutoId()))
                .body(toResponse(produto));
    }

    @GetMapping("/{produtoId}")
    ProdutoResponse buscarProduto(@PathVariable String produtoId) {
        return toResponse(estoqueService.buscarProduto(produtoId));
    }

    @GetMapping
    List<ProdutoResponse> listarProdutos() {
        return estoqueService.listarProdutos().stream()
                .map(ProdutoController::toResponse)
                .toList();
    }

    @PostMapping("/{produtoId}/estoque-ajustes")
    ProdutoResponse ajustarEstoque(
            @PathVariable String produtoId,
            @Valid @RequestBody AjustarEstoqueRequest request
    ) {
        return toResponse(estoqueService.ajustarEstoque(produtoId, request.delta()));
    }

    private static ProdutoResponse toResponse(Produto produto) {
        return new ProdutoResponse(
                produto.getProdutoId(),
                produto.getNome(),
                produto.getPrecoUnitario(),
                produto.getQuantidadeDisponivelTotal(),
                produto.getQuantidadeReservada(),
                produto.getQuantidadeDisponivel(),
                produto.getLimiteEstoqueBaixo(),
                produto.isEstoqueBaixo()
        );
    }
}
