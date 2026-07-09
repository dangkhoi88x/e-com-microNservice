package com.example.inventoryservice.dto.request;

import jakarta.validation.constraints.NotBlank;

public record InventoryOrderRequest(
        @NotBlank(message = "orderId is required")
        String orderId
) {
}
