package com.marketflow.pedido.repository;

import com.marketflow.pedido.domain.OutboxEvent;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

public interface OutboxEventRepository extends JpaRepository<OutboxEvent, UUID> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select e from OutboxEvent e where e.publicado = false order by e.criadoEm")
    List<OutboxEvent> findPendentesForUpdate(Pageable pageable);
}
