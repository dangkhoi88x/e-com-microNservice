package com.example.sellerservice.dto.response;

import com.example.sellerservice.entity.SellerStatus;

import java.time.Instant;
import java.util.UUID;

public record SellerShopResponse(
        UUID id,
        String ownerUserId,
        String slug,
        String shopName,
        String description,
        String phone,
        String address,
        String city,
        SellerStatus status,
        String reviewNote,
        String reviewedBy,
        Instant reviewedAt,
        Instant createdAt,
        Instant updatedAt
) {
}
