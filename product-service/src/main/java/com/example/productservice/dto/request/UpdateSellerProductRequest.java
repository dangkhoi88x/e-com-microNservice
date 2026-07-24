package com.example.productservice.dto.request;

import com.example.productservice.entity.ProductImage;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.List;

public record UpdateSellerProductRequest(
        String categoryId,
        @Size(min = 1) String name,
        String description,
        @DecimalMin(value = "0.0", inclusive = false) BigDecimal price,
        @Min(0) Integer quantity,
        List<ProductImage> images,
        List<ProductOptionRequest> options,
        List<ProductVariantRequest> variants
) {
}
