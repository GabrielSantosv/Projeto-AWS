package com.marketflow.inventory.event.dto;

import java.util.List;

public record EmployeeValidatedPayload(
        String orderId,
        String employeeId,
        List<OrderItem> items
) {
}
