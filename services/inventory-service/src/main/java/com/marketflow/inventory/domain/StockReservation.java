package com.marketflow.inventory.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
        name = "stock_reservations",
        uniqueConstraints = @UniqueConstraint(name = "uk_stock_reservations_order_product", columnNames = {"order_id", "product_id"})
)
public class StockReservation {

    @Id
    private UUID id;

    @Column(name = "order_id", nullable = false)
    private String orderId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(nullable = false)
    private int quantity;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ReservationStatus status;

    @Column(nullable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    protected StockReservation() {
    }

    public StockReservation(String orderId, Product product, int quantity) {
        this.id = UUID.randomUUID();
        this.orderId = orderId;
        this.product = product;
        this.quantity = quantity;
        this.status = ReservationStatus.RESERVED;
        this.createdAt = Instant.now();
        this.updatedAt = this.createdAt;
    }

    public boolean isReserved() {
        return status == ReservationStatus.RESERVED;
    }

    public Product getProduct() {
        return product;
    }

    public int getQuantity() {
        return quantity;
    }

    public ReservationStatus getStatus() {
        return status;
    }

    public void confirm() {
        status = ReservationStatus.CONFIRMED;
        updatedAt = Instant.now();
    }

    public void release() {
        status = ReservationStatus.RELEASED;
        updatedAt = Instant.now();
    }
}
