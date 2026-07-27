package com.example.sellerservice.dto.response;

import com.example.sellerservice.entity.SellerStatus;

import java.util.UUID;

public record SellerEligibilityResponse(
        boolean approved,
        UUID shopId,
        SellerStatus status
) {
}
