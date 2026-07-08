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
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "stock_movements")
public class StockMovement {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StockMovementType type;

    @Column(nullable = false)
    private int quantity;

    @Column(nullable = false)
    private int previousQuantityOnHand;

    @Column(nullable = false)
    private int newQuantityOnHand;

    @Column(nullable = false)
    private String reason;

    @Column(nullable = false)
    private Instant createdAt;

    protected StockMovement() {
    }

    public StockMovement(
            Product product,
            StockMovementType type,
            int quantity,
            int previousQuantityOnHand,
            int newQuantityOnHand,
            String reason
    ) {
        this.id = UUID.randomUUID();
        this.product = product;
        this.type = type;
        this.quantity = quantity;
        this.previousQuantityOnHand = previousQuantityOnHand;
        this.newQuantityOnHand = newQuantityOnHand;
        this.reason = reason;
        this.createdAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public Product getProduct() {
        return product;
    }

    public StockMovementType getType() {
        return type;
    }

    public int getQuantity() {
        return quantity;
    }

    public int getPreviousQuantityOnHand() {
        return previousQuantityOnHand;
    }

    public int getNewQuantityOnHand() {
        return newQuantityOnHand;
    }

    public String getReason() {
        return reason;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
