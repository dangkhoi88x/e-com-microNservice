package com.example.wishlistservice.dto.request;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;

public record AddWishlistItemRequest(@NotBlank String productId, String variantId, @NotBlank String productName, @NotNull @PositiveOrZero BigDecimal price, String imageUrl, String categoryName) { }
