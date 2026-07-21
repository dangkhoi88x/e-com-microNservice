package com.example.promotionservice.dto.request;

import jakarta.validation.constraints.NotBlank;

public record FlashDealOrderRequest(@NotBlank String orderId) {}
