package com.example.cartservice.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record AddCartItemRequest(
        @NotBlank String productId,
        String variantId,
        @NotNull @Min(1) Integer quantity
) {}