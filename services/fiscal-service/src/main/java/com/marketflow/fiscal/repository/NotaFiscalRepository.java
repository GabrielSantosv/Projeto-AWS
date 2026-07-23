package com.marketflow.fiscal.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.marketflow.fiscal.domain.NotaFiscal;

@Repository
public interface NotaFiscalRepository extends JpaRepository<NotaFiscal, java.util.UUID> {

    Optional<NotaFiscal> findByPedidoId(String pedidoId);
}
