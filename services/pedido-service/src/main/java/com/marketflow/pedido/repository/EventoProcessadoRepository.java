package com.marketflow.pedido.repository;

import com.marketflow.pedido.domain.EventoProcessado;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EventoProcessadoRepository extends JpaRepository<EventoProcessado, String> {
}
