package com.marketflow.inventory.event.dto;

public record InvoiceRejectedPayload(
        String orderId,
        String reason
) {
}
