package com.example.reviewservice.service;

import com.example.reviewservice.dto.request.CreateReviewRequest;
import com.example.reviewservice.dto.request.ModerateReviewRequest;
import com.example.reviewservice.dto.request.SellerReplyRequest;
import com.example.reviewservice.dto.request.UpdateReviewRequest;
import com.example.reviewservice.dto.response.PageResponse;
import com.example.reviewservice.dto.response.ProductReviewResponse;
import com.example.reviewservice.dto.response.ReviewSummaryResponse;
import com.example.reviewservice.entity.ReviewStatus;

import java.time.Instant;
import java.util.UUID;

public interface ReviewService {
    ProductReviewResponse createReview(String userId, String authorization, CreateReviewRequest request);
    ProductReviewResponse updateReview(String userId, UUID reviewId, UpdateReviewRequest request);
    void deleteReview(String userId, UUID reviewId);
    ProductReviewResponse moderateReview(UUID reviewId, ModerateReviewRequest request);
    ProductReviewResponse replyReview(UUID reviewId, SellerReplyRequest request);
    PageResponse<ProductReviewResponse> getProductReviews(String productId, int page, int size);
    PageResponse<ProductReviewResponse> getMyReviews(String userId, int page, int size);
    PageResponse<ProductReviewResponse> getSellerReviews(String sellerId, int page, int size);
    ProductReviewResponse replySellerReview(String sellerId, UUID reviewId, SellerReplyRequest request);
    ReviewSummaryResponse getProductSummary(String productId);
    ProductReviewResponse getReview(UUID reviewId);
    ProductReviewResponse getMyReviewByOrderItem(String userId, String orderItemId);
    PageResponse<ProductReviewResponse> getAdminReviews(
            ReviewStatus status,
            Integer rating,
            String productId,
            String userId,
            Instant createdFrom,
            Instant createdTo,
            int page,
            int size
    );
}
