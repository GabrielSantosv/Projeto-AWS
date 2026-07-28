package com.marketflow.funcionario.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.marketflow.funcionario.domain.SessaoCaixa;

public interface SessaoCaixaRepository extends JpaRepository<SessaoCaixa, UUID> {

    Optional<SessaoCaixa> findByTokenId(String tokenId);
}
