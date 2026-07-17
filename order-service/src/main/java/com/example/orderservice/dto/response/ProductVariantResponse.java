package com.example.orderservice.dto.response;

import java.math.BigDecimal;
import java.util.Map;

public record ProductVariantResponse(
        String id,
        String sku,
        Map<String, String> attributes,
        BigDecimal price,
        Integer quantity,
        String imageUrl,
        String status
) {
}
