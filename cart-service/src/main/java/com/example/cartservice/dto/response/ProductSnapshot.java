package com.example.cartservice.dto.response;

import java.math.BigDecimal;

public record ProductSnapshot(
        String productId,
        String variantId,
        String productName,
        String variantName,
        BigDecimal price,
        String imageUrl
) {
}
