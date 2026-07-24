package com.example.orderservice.dto.response;

import java.time.Instant;
import java.util.List;

public record SellerOrderDetailResponse(OrderResponse order, Shipment shipment) {
    public record Shipment(String id, String carrier, String trackingNumber, String status, Instant estimatedDeliveryAt, Instant shippedAt, Instant deliveredAt, List<TimelineItem> timeline) { }
    public record TimelineItem(String status, String description, Instant occurredAt) { }
}
