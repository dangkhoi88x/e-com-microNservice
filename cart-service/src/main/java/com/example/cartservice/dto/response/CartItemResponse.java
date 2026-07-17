package com.example.cartservice.dto.response;

import lombok.Builder;

import java.math.BigDecimal;

@Builder
public record CartItemResponse(
        String id,
        String productId,
        String variantId,
        String productName,
        String variantName,
        BigDecimal price,
        String imageUrl,
        Integer quantity,
        Boolean selected,
        BigDecimal subtotal
) {
}
