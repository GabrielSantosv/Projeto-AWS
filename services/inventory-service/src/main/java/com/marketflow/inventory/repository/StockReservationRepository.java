package com.marketflow.inventory.repository;

import com.marketflow.inventory.domain.ReservationStatus;
import com.marketflow.inventory.domain.StockReservation;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StockReservationRepository extends JpaRepository<StockReservation, UUID> {

    List<StockReservation> findByOrderId(String orderId);

    List<StockReservation> findByOrderIdAndStatus(String orderId, ReservationStatus status);

    Optional<StockReservation> findByOrderIdAndProductId(String orderId, UUID productId);
}
