package com.marketflow.expedicao.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.marketflow.expedicao.domain.Separacao;

public interface SeparacaoRepository extends JpaRepository<Separacao, UUID> {

    Optional<Separacao> findByPedidoId(String pedidoId);
}
