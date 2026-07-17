package com.example.productservice.dto.response;

import com.example.productservice.common.ProductStatus;
import lombok.Builder;

import java.math.BigDecimal;
import java.util.Map;

@Builder
public record ProductVariantResponse(
        String id,
        String sku,
        Map<String, String> attributes,
        BigDecimal price,
        Integer quantity,
        String imageUrl,
        ProductStatus status
) {
}
