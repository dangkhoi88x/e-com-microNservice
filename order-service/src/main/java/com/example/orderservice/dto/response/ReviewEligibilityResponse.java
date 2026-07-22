package com.example.orderservice.dto.response;

public record ReviewEligibilityResponse(
        boolean eligible,
        String orderId,
        String orderItemId,
        String productId,
        String variantId,
        String productName,
        String orderStatus
) {
}
