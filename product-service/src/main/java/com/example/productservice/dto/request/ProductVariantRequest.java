package com.example.productservice.dto.request;

import com.example.productservice.common.ProductStatus;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;

import java.math.BigDecimal;
import java.util.Map;

public record ProductVariantRequest(
        String id,

        String sku,

        Map<String, String> attributes,

        @DecimalMin(value = "0.0", inclusive = false, message = "Variant price must be greater than 0")
        BigDecimal price,

        @Min(value = 0, message = "Variant quantity must be greater than or equal to 0")
        Integer quantity,

        String imageUrl,

        ProductStatus status
) {
}
