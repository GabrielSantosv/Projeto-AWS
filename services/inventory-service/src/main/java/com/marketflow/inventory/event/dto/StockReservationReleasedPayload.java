package com.marketflow.inventory.event.dto;

import java.util.List;

public record StockReservationReleasedPayload(
        String orderId,
        String reason,
        List<OrderItem> items
) {
}
