package com.example.productservice.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.Map;

public record CreateSellerProductVariantRequest(
        @NotBlank(message = "Variant SKU is required")
        String sku,

        Map<String, String> attributes,

        @NotNull
        @DecimalMin(value = "0.0", inclusive = false, message = "Variant price must be greater than 0")
        BigDecimal price,

        @NotNull
        @Min(value = 0, message = "Variant quantity must be greater than or equal to 0")
        Integer quantity,

        String imageUrl
) {
}
