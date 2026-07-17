package com.example.inventoryservice.dto.response;

import java.time.Instant;

public record InventoryResponse(
        String id,
        String productId,
        String variantId,
        Integer availableQuantity,
        Integer reservedQuantity,
        Integer soldQuantity,
        Instant createdAt,
        Instant updatedAt
) {
}
