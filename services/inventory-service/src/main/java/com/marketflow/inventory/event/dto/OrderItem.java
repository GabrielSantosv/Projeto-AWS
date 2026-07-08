package com.marketflow.inventory.event.dto;

import java.util.UUID;

public record OrderItem(UUID productId, int quantity) {
}
