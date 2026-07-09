package com.example.inventoryservice.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record ReserveInventoryRequest(
        @NotBlank(message = "orderId is required")
        String orderId,

        @Valid
        @NotEmpty(message = "items is required")
        List<ReserveInventoryItemRequest> items
) {
}
