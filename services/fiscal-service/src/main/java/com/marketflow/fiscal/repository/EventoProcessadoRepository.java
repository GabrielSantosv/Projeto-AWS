package com.marketflow.fiscal.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.marketflow.fiscal.domain.EventoProcessado;

@Repository
public interface EventoProcessadoRepository extends JpaRepository<EventoProcessado, String> {
}
