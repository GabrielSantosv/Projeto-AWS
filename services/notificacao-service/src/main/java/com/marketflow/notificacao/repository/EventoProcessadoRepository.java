package com.marketflow.notificacao.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.marketflow.notificacao.domain.EventoProcessado;

public interface EventoProcessadoRepository extends JpaRepository<EventoProcessado, String> {
}
