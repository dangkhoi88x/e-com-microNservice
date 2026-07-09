package com.example.productservice.dto.response;

import java.time.Instant;

public record InventoryResponse(
        String id,
        String productId,
        Integer availableQuantity,
        Integer reservedQuantity,
        Integer soldQuantity,
        Instant createdAt,
        Instant updatedAt
) {
}
