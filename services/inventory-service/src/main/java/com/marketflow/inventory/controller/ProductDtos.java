package com.marketflow.inventory.controller;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.UUID;

public final class ProductDtos {

    private ProductDtos() {
    }

    public record CreateProductRequest(
            @NotBlank String sku,
            @NotBlank String name,
            @NotNull @DecimalMin("0.00") BigDecimal unitPrice,
            @Min(0) int quantityOnHand,
            @Min(0) int lowStockThreshold
    ) {
    }

    public record AdjustStockRequest(
            int delta
    ) {
    }

    public record ProductResponse(
            UUID id,
            String sku,
            String name,
            BigDecimal unitPrice,
            int quantityOnHand,
            int quantityReserved,
            int quantityAvailable,
            int lowStockThreshold
    ) {
    }
}
