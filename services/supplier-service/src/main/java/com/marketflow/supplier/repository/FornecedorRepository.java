package com.marketflow.supplier.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.marketflow.supplier.domain.Fornecedor;

public interface FornecedorRepository extends JpaRepository<Fornecedor, UUID> {

    List<Fornecedor> findByProdutoIdAndAtivoTrueOrderByPrecoAsc(String produtoId);
}
