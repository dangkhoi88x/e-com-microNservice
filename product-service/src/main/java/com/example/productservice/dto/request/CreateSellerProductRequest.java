package com.example.productservice.dto.request;

import com.example.productservice.entity.ProductImage;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.List;

public record CreateSellerProductRequest(
        @NotBlank String categoryId,
        @NotBlank String name,
        String description,
        @NotNull @DecimalMin(value = "0.0", inclusive = false) BigDecimal price,
        @NotNull @Min(0) Integer quantity,
        List<ProductImage> images,
        List<ProductOptionRequest> options,
        List<ProductVariantRequest> variants
) {
}
