package com.marketflow.inventory.event.dto;

import java.util.List;

public record StockReservedPayload(
        String orderId,
        List<OrderItem> items
) {
}
