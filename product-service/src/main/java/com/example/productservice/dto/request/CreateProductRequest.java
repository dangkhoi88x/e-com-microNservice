package com.example.productservice.dto.request;

import com.example.productservice.common.ProductStatus;
import com.example.productservice.entity.ProductImage;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.List;

public record CreateProductRequest(
        @NotBlank(message = "Category id is required")
        String categoryId,

        @NotBlank(message = "Name is required")
        String name,

        String description,

        @NotNull(message = "Price is required")
        @DecimalMin(value = "0.0", message = "Price must be greater than or equal to 0.0")
        BigDecimal price,

        @NotNull(message = "Quantity is required")
        @Min(value = 0, message = "Quantity must be greater than or equal to 0")
        Integer quantity,

        List<ProductImage> images,

        @NotNull(message = "Status is required")
        ProductStatus status

) {
}
