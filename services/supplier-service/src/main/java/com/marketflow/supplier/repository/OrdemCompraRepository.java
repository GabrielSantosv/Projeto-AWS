package com.marketflow.supplier.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.marketflow.supplier.domain.OrdemCompra;

public interface OrdemCompraRepository extends JpaRepository<OrdemCompra, UUID> {

    List<OrdemCompra> findByProdutoIdOrderByCriadaEmDesc(String produtoId);
}
