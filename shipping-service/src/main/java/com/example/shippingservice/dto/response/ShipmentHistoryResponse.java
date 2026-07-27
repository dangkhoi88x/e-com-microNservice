package com.example.shippingservice.dto.response;

import com.example.shippingservice.entity.ShipmentStatus;

import java.time.Instant;
import java.util.UUID;

public record ShipmentHistoryResponse(
        UUID id,
        UUID shipmentId,
        ShipmentStatus status,
        String description,
        String location,
        Instant occurredAt
) {
}
