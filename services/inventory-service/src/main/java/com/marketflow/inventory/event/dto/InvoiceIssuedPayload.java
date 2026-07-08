package com.marketflow.inventory.event.dto;

public record InvoiceIssuedPayload(
        String orderId,
        String invoiceNumber
) {
}
