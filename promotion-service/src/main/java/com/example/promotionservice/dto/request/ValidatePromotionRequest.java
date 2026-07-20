package com.example.promotionservice.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record ValidatePromotionRequest(
        @NotBlank String campaignCode,
        @NotNull BigDecimal subtotalAmount
) {}
