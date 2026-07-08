package com.marketflow.inventory.event.dto;

import java.util.List;

public record StockConfirmedPayload(
        String orderId,
        List<OrderItem> items
) {
}
