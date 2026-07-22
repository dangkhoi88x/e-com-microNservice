package com.example.shippingservice.dto.response;

import com.example.shippingservice.entity.ShipmentStatus;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record ShipmentResponse(
        UUID id,
        String orderId,
        String userId,
        String shippingAddress,
        String carrier,
        String trackingNumber,
        ShipmentStatus status,
        Instant estimatedDeliveryAt,
        Instant shippedAt,
        Instant deliveredAt,
        List<ShipmentHistoryResponse> timeline,
        Instant createdAt,
        Instant updatedAt
) {
}
