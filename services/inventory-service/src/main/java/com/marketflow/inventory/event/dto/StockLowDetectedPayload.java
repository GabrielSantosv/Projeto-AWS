package com.marketflow.inventory.event.dto;

import java.util.UUID;

public record StockLowDetectedPayload(
        UUID productId,
        String sku,
        String productName,
        int quantityAvailable,
        int lowStockThreshold
) {
}
