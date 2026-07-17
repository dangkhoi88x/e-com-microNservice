package com.example.productservice.dto.response;

import com.example.productservice.common.ProductStatus;
import com.example.productservice.entity.ProductImage;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Builder
public record CreateProductResponse(
        String id,
        String name,
        String slug,
        String description,
        BigDecimal price,
        Integer quantity,
        List<ProductImage> images,
        List<ProductOptionResponse> options,
        List<ProductVariantResponse> variants,
        ProductStatus status,
        Instant createdAt

) {
}
