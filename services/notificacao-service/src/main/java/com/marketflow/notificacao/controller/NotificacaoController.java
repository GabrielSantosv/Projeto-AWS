package com.marketflow.notificacao.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.marketflow.notificacao.repository.NotificacaoRepository;

@RestController
@RequestMapping("/notificacoes")
public class NotificacaoController {

    private final NotificacaoRepository notificacaoRepository;

    public NotificacaoController(NotificacaoRepository notificacaoRepository) {
        this.notificacaoRepository = notificacaoRepository;
    }

    @GetMapping("/{pedidoId}")
    public List<NotificacaoDtos.Resposta> buscarPorPedido(@PathVariable String pedidoId) {
        return notificacaoRepository.findByPedidoIdOrderByEnviadaEmAsc(pedidoId).stream()
                .map(NotificacaoDtos.Resposta::from)
                .toList();
    }
}
