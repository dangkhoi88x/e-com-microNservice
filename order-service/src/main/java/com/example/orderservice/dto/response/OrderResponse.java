package com.example.orderservice.dto.response;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record OrderResponse(
        String id,
        String userId,
        BigDecimal totalAmount,
        String status,
        String shippingAddress,
        List<OrderItemResponse> items,
        Instant createdAt
) {
}
