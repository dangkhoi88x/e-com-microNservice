package com.example.promotionservice.dto.request;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;

public record FlashDealItemRequest(@NotBlank String productId, String variantId,
                                   @NotNull @DecimalMin("0.01") BigDecimal originalPrice,
                                   @NotNull @DecimalMin("0") @DecimalMax("100") BigDecimal discountPercent,
                                   @Positive Integer quota, Boolean quotaLimited) {}
