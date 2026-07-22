package com.example.shippingservice.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateShipmentRequest(
        @NotBlank(message = "orderId is required")
        String orderId,

        @NotBlank(message = "userId is required")
        String userId,

        @NotBlank(message = "shippingAddress is required")
        @Size(max = 1000, message = "shippingAddress must not exceed 1000 characters")
        String shippingAddress
) {
}
