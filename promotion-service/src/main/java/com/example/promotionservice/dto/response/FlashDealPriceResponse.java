package com.example.promotionservice.dto.response;

import java.math.BigDecimal;
import java.util.UUID;

public record FlashDealPriceResponse(UUID flashDealItemId, String productId, String variantId, BigDecimal originalPrice, BigDecimal salePrice, Integer quantity) {}
