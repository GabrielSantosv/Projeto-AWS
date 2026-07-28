package com.marketflow.expedicao.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.marketflow.expedicao.domain.EventoProcessado;

public interface EventoProcessadoRepository extends JpaRepository<EventoProcessado, String> {
}
