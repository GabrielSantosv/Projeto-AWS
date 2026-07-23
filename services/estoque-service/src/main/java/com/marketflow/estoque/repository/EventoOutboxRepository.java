package com.marketflow.estoque.repository;

import com.marketflow.estoque.domain.EventoOutbox;
import com.marketflow.estoque.domain.StatusOutbox;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

public interface EventoOutboxRepository extends JpaRepository<EventoOutbox, UUID> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select e from EventoOutbox e where e.status = :status order by e.criadoEm")
    List<EventoOutbox> findByStatusForUpdate(StatusOutbox status, Pageable pageable);
}
