package com.example.orderservice.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record OrderItemRequest(
        @NotBlank(message = "Product id cannot be blank")
        String productId,

        @NotNull(message = "Quantity is required")
        @Min(value = 1, message = "Quantity must be greater than 0")
        Integer quantity

) {
}
