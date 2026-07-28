package com.marketflow.notificacao.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.marketflow.notificacao.domain.Notificacao;

public interface NotificacaoRepository extends JpaRepository<Notificacao, UUID> {

    List<Notificacao> findByPedidoIdOrderByEnviadaEmAsc(String pedidoId);
}
