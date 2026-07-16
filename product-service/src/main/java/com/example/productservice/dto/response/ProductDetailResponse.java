package com.example.productservice.dto.response;

import com.example.productservice.common.ProductStatus;
import com.example.productservice.entity.ProductImage;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Builder
public record ProductDetailResponse(
        String id,
        String name,
        String slug,
        String description,
        String categoryId,
        String categoryName,
        BigDecimal price,
        Integer quantity,
        List<ProductImage> images,
        ProductStatus status,
        Instant createdAt

) {
}
