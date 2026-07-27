package com.example.reviewservice.dto.response;

import com.example.reviewservice.entity.ReviewStatus;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record ProductReviewResponse(
        UUID id,
        String productId,
        String variantId,
        String reviewerName,
        boolean verifiedPurchase,
        String orderId,
        String orderItemId,
        Integer rating,
        String content,
        List<String> images,
        ReviewStatus status,
        String sellerReply,
        Instant createdAt,
        Instant updatedAt
) {
}
