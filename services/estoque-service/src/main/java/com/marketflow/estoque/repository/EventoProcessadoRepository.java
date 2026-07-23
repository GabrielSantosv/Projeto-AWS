package com.marketflow.estoque.repository;

import com.marketflow.estoque.domain.EventoProcessado;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EventoProcessadoRepository extends JpaRepository<EventoProcessado, String> {
}
