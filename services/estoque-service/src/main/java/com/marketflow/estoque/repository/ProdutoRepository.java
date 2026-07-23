package com.marketflow.estoque.repository;

import com.marketflow.estoque.domain.Produto;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

public interface ProdutoRepository extends JpaRepository<Produto, String> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Produto p where p.produtoId = :produtoId")
    Optional<Produto> findByIdForUpdate(String produtoId);
}
