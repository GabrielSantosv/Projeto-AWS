package com.marketflow.fiscal.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.marketflow.fiscal.domain.NotaFiscal;
import com.marketflow.fiscal.repository.NotaFiscalRepository;

@RestController
@RequestMapping("/notas-fiscais")
public class NotaFiscalController {

    private final NotaFiscalRepository notaFiscalRepository;

    public NotaFiscalController(NotaFiscalRepository notaFiscalRepository) {
        this.notaFiscalRepository = notaFiscalRepository;
    }

    @GetMapping
    public List<NotaFiscal> listar() {
        return notaFiscalRepository.findAll();
    }

    @GetMapping("/{pedidoId}")
    public NotaFiscal buscarPorPedido(@PathVariable String pedidoId) {
        return notaFiscalRepository.findByPedidoId(pedidoId).orElseThrow();
    }
}
