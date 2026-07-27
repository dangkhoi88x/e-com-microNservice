package com.example.reviewservice.dto.response;

import java.util.Map;

public record ReviewSummaryResponse(
        String productId,
        double averageRating,
        long reviewCount,
        Map<Integer, Long> ratingDistribution
) {
}
