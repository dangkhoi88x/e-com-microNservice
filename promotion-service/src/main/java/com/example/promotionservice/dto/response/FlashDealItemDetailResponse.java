package com.example.promotionservice.dto.response;

import java.math.BigDecimal;
import java.util.UUID;

public record FlashDealItemDetailResponse(UUID id, String productId, String variantId,
                                          BigDecimal originalPrice, BigDecimal salePrice, BigDecimal discountPercent,
                                          boolean quotaLimited, Integer initialQuota, Integer usedQuota, Integer remainingQuota,
                                          long orderCount, BigDecimal revenue) {}
