package com.example.inventoryservice.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record BatchInventoryRequest(
        @NotEmpty(message = "productIds is required")
        List<@NotBlank(message = "productId must not be blank") String> productIds
) {
}
