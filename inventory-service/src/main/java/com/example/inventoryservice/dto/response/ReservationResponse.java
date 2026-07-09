package com.example.inventoryservice.dto.response;

import com.example.inventoryservice.enums.ReservationStatus;

import java.time.Instant;

public record ReservationResponse(
        String id,
        String orderId,
        String productId,
        Integer quantity,
        ReservationStatus status,
        Instant createdAt,
        Instant updatedAt
) {
}
