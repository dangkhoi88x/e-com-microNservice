package com.example.inventoryservice.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateInventoryRequest(
        @NotBlank(message = "productId is required")
        String productId,

        @NotNull(message = "availableQuantity is required")
        @Min(value = 0, message = "availableQuantity must be greater than or equal to 0")
        Integer availableQuantity
) {
}