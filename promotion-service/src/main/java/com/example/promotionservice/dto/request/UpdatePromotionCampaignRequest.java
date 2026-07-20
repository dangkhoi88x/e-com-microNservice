package com.example.promotionservice.dto.request;

import com.example.promotionservice.entity.PromotionStatus;
import com.example.promotionservice.entity.PromotionType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record UpdatePromotionCampaignRequest(
        @NotBlank String name,
        String description,
        @NotNull PromotionType type,
        @NotNull @DecimalMin("0.01") BigDecimal discountValue,
        @DecimalMin("0.00") BigDecimal maxDiscountAmount,
        @NotNull @PositiveOrZero BigDecimal minOrderAmount,
        @NotNull Instant startAt,
        @NotNull Instant endAt,
        @NotNull @PositiveOrZero Integer usageLimit,
        List<String> applicableCategoryIds,
        List<String> applicableProductIds,
        @PositiveOrZero Integer priority,
        Boolean stackable,
        @NotNull PromotionStatus status
) {}
