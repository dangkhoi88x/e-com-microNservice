package com.example.promotionservice.dto.response;

import java.math.BigDecimal;
import java.util.UUID;

public record FlashDealItemResponse(UUID id, String productId, String variantId,
                                    BigDecimal originalPrice, BigDecimal salePrice, BigDecimal discountPercent,
                                    Integer quota, Integer initialQuota, boolean quotaLimited) {}
