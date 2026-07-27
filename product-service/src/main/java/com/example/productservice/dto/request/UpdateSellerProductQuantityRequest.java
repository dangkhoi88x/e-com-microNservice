package com.example.productservice.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record UpdateSellerProductQuantityRequest(
        @NotNull @Min(0) Integer quantity
) {
}
