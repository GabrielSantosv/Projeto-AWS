package com.marketflow.supplier.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;

import com.marketflow.supplier.service.SupplierService;

@RestController
@RequestMapping("/fornecedores")
public class FornecedorController {

    private final SupplierService supplierService;

    public FornecedorController(SupplierService supplierService) {
        this.supplierService = supplierService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public FornecedorDtos.Resposta criar(@Valid @RequestBody FornecedorDtos.CadastroRequest request) {
        return FornecedorDtos.Resposta.from(supplierService.criarFornecedor(
                request.produtoId(),
                request.nome(),
                request.preco(),
                request.prazoDias(),
                request.ativo()
        ));
    }

    @GetMapping
    public List<FornecedorDtos.Resposta> listar() {
        return supplierService.listarFornecedores().stream()
                .map(FornecedorDtos.Resposta::from)
                .toList();
    }

    @GetMapping("/{id}")
    public FornecedorDtos.Resposta buscar(@PathVariable UUID id) {
        return FornecedorDtos.Resposta.from(supplierService.buscarFornecedor(id));
    }

    @PutMapping("/{id}")
    public FornecedorDtos.Resposta atualizar(
            @PathVariable UUID id,
            @Valid @RequestBody FornecedorDtos.CadastroRequest request
    ) {
        return FornecedorDtos.Resposta.from(supplierService.atualizarFornecedor(
                id,
                request.nome(),
                request.preco(),
                request.prazoDias(),
                request.ativo()
        ));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void remover(@PathVariable UUID id) {
        supplierService.removerFornecedor(id);
    }
}
