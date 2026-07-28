package com.marketflow.supplier.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.marketflow.supplier.domain.EventoProcessado;

public interface EventoProcessadoRepository extends JpaRepository<EventoProcessado, String> {
}
