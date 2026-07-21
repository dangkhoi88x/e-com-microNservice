package com.example.promotionservice.dto.request;

import jakarta.validation.constraints.*;

public record FlashDealOrderItemRequest(@NotBlank String productId, String variantId, @NotNull @Positive Integer quantity) {}
