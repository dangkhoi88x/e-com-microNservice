package com.example.shippingservice.dto.request;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.Instant;

public record AssignCarrierRequest(
        @NotBlank(message = "carrier is required")
        @Size(max = 100, message = "carrier must not exceed 100 characters")
        String carrier,

        @NotBlank(message = "trackingNumber is required")
        @Size(max = 100, message = "trackingNumber must not exceed 100 characters")
        String trackingNumber,

        @Future(message = "estimatedDeliveryAt must be in the future")
        Instant estimatedDeliveryAt
) {
}
