package com.example.orderservice.dto.response;

import java.math.BigDecimal;

public record PromotionCalculationResponse(
        boolean eligible,
        String campaignId,
        String campaignCode,
        BigDecimal discountAmount,
        BigDecimal finalAmount,
        String message
) {}
