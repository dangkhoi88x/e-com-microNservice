package com.example.orderservice.dto.response;

import java.math.BigDecimal;

public record OrderItemResponse(
        String productId,
        String productName,
        BigDecimal price,
        Integer quantity,
        BigDecimal subtotal
) {
}
