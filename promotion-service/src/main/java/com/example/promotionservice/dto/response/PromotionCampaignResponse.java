package com.example.promotionservice.dto.response;

import com.example.promotionservice.entity.PromotionStatus;
import com.example.promotionservice.entity.PromotionType;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record PromotionCampaignResponse(
        String id,
        String name,
        String code,
        String description,
        PromotionType type,
        BigDecimal discountValue,
        BigDecimal maxDiscountAmount,
        BigDecimal minOrderAmount,
        Instant startAt,
        Instant endAt,
        Integer usageLimit,
        Integer usedCount,
        PromotionStatus status,
        List<String> applicableCategoryIds,
        List<String> applicableProductIds,
        Integer priority,
        boolean stackable,
        Instant createdAt,
        Instant updatedAt
) {}
