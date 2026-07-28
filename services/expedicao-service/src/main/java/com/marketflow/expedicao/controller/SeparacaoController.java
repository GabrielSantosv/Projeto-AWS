package com.marketflow.expedicao.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.marketflow.expedicao.exception.SeparacaoNaoEncontradaException;
import com.marketflow.expedicao.repository.SeparacaoRepository;

@RestController
@RequestMapping("/separacoes")
public class SeparacaoController {

    private final SeparacaoRepository separacaoRepository;

    public SeparacaoController(SeparacaoRepository separacaoRepository) {
        this.separacaoRepository = separacaoRepository;
    }

    @GetMapping("/{pedidoId}")
    public SeparacaoDtos.Resposta buscarPorPedido(@PathVariable String pedidoId) {
        return separacaoRepository.findByPedidoId(pedidoId)
                .map(SeparacaoDtos.Resposta::from)
                .orElseThrow(() -> new SeparacaoNaoEncontradaException(pedidoId));
    }
}
