package com.example.shippingservice.dto.request;

import jakarta.validation.constraints.Size;

public record UpdateShipmentStatusRequest(
        @Size(max = 500, message = "description must not exceed 500 characters")
        String description,

        @Size(max = 200, message = "location must not exceed 200 characters")
        String location
) {
}
