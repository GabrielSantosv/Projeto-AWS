package com.marketflow.inventory.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "products")
public class Product {

    @Id
    private UUID id;

    @Column(nullable = false, unique = true)
    private String sku;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal unitPrice;

    @Column(nullable = false)
    private int quantityOnHand;

    @Column(nullable = false)
    private int quantityReserved;

    @Column(nullable = false)
    private int lowStockThreshold;

    @Column(nullable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    protected Product() {
    }

    public Product(String sku, String name, BigDecimal unitPrice, int quantityOnHand, int lowStockThreshold) {
        this.id = UUID.randomUUID();
        this.sku = sku;
        this.name = name;
        this.unitPrice = unitPrice;
        this.quantityOnHand = quantityOnHand;
        this.quantityReserved = 0;
        this.lowStockThreshold = lowStockThreshold;
        this.createdAt = Instant.now();
        this.updatedAt = this.createdAt;
    }

    public int getQuantityAvailable() {
        return quantityOnHand - quantityReserved;
    }

    public UUID getId() {
        return id;
    }

    public String getSku() {
        return sku;
    }

    public String getName() {
        return name;
    }

    public BigDecimal getUnitPrice() {
        return unitPrice;
    }

    public int getQuantityOnHand() {
        return quantityOnHand;
    }

    public int getQuantityReserved() {
        return quantityReserved;
    }

    public int getLowStockThreshold() {
        return lowStockThreshold;
    }

    public boolean canReserve(int quantity) {
        return quantity > 0 && getQuantityAvailable() >= quantity;
    }

    public void reserve(int quantity) {
        if (!canReserve(quantity)) {
            throw new IllegalStateException("Insufficient available stock");
        }
        quantityReserved += quantity;
        touch();
    }

    public void confirmReserved(int quantity) {
        if (quantityReserved < quantity) {
            throw new IllegalStateException("Reserved stock cannot be negative");
        }
        quantityReserved -= quantity;
        quantityOnHand -= quantity;
        touch();
    }

    public void releaseReserved(int quantity) {
        if (quantityReserved < quantity) {
            throw new IllegalStateException("Reserved stock cannot be negative");
        }
        quantityReserved -= quantity;
        touch();
    }

    public void addStock(int quantity) {
        validatePositiveQuantity(quantity);
        quantityOnHand += quantity;
        touch();
    }

    public void removeStock(int quantity) {
        validatePositiveQuantity(quantity);
        adjustStock(-quantity);
    }

    public void adjustStock(int delta) {
        if (delta == 0) {
            throw new IllegalArgumentException("Stock adjustment delta cannot be zero");
        }
        int nextQuantityOnHand = quantityOnHand + delta;
        if (nextQuantityOnHand < quantityReserved) {
            throw new IllegalArgumentException("Adjustment would make stock lower than reserved quantity");
        }
        quantityOnHand = nextQuantityOnHand;
        touch();
    }

    private void validatePositiveQuantity(int quantity) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("Quantity must be greater than zero");
        }
    }

    private void touch() {
        updatedAt = Instant.now();
    }
}
