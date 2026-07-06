package com.example.productservice.dto.request;

import com.example.productservice.common.ProductStatus;
import com.example.productservice.entity.ProductImage;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.List;

public record UpdateProductRequest(
        String categoryId,

        @Size(min = 1, message = "Name must not be empty")
        String name,

        String description,

        @DecimalMin(value = "0.0", inclusive = false, message = "Price must be greater than 0")
        BigDecimal price,

        @Min(value = 0, message = "Quantity must be greater than or equal to 0")
        Integer quantity,

        List<ProductImage> images,

        ProductStatus status
) {
}
