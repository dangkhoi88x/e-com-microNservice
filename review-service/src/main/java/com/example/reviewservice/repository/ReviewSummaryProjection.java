package com.example.reviewservice.repository;

public interface ReviewSummaryProjection {
    Double getAverageRating();
    Long getReviewCount();
}
