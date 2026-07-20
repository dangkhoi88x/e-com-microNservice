package com.example.orderservice.dto.response;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record OrderResponse(
        String id,
        String orderCode,
        String userId,
        BigDecimal subtotalAmount,
        BigDecimal discountAmount,
        String promotionCode,
        BigDecimal totalAmount,
        String status,
        String shippingAddress,
        List<OrderItemResponse> items,
        Instant createdAt
) {
}
