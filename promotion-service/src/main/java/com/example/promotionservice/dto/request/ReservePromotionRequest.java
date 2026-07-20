package com.example.promotionservice.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record ReservePromotionRequest(
        @NotBlank String campaignCode,
        @NotBlank String userId,
        @NotBlank String orderId,
        @NotNull BigDecimal subtotalAmount
) {}
