package com.example.wishlistservice.dto.response;

import java.math.BigDecimal;
import java.time.Instant;
public record WishlistItemResponse(String id, String productId, String variantId, String productName, BigDecimal price, String imageUrl, String categoryName, Instant createdAt) { }
