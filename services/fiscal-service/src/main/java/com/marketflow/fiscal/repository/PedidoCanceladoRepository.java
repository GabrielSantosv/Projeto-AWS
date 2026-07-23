package com.marketflow.fiscal.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.marketflow.fiscal.domain.PedidoCancelado;

@Repository
public interface PedidoCanceladoRepository extends JpaRepository<PedidoCancelado, java.util.UUID> {

    Optional<PedidoCancelado> findByPedidoId(String pedidoId);
}
