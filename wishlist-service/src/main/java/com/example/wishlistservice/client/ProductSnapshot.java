package com.example.wishlistservice.client;
import java.math.BigDecimal;
public record ProductSnapshot(String productId, String variantId, String productName, BigDecimal price, String imageUrl, String categoryName) { }
