package com.marketflow.estoque.repository;

import com.marketflow.estoque.domain.ReservaEstoque;
import com.marketflow.estoque.domain.StatusReserva;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReservaEstoqueRepository extends JpaRepository<ReservaEstoque, UUID> {

    List<ReservaEstoque> findByPedidoId(String pedidoId);

    List<ReservaEstoque> findByPedidoIdAndStatus(String pedidoId, StatusReserva status);
}
