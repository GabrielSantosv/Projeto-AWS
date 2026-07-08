package com.marketflow.inventory.event.dto;

public record BillingRejectedPayload(
        String orderId,
        String reason
) {
}
