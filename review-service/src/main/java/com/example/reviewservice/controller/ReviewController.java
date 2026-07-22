package com.example.reviewservice.controller;

import com.example.reviewservice.dto.request.CreateReviewRequest;
import com.example.reviewservice.dto.request.ModerateReviewRequest;
import com.example.reviewservice.dto.request.SellerReplyRequest;
import com.example.reviewservice.dto.request.UpdateReviewRequest;
import com.example.reviewservice.dto.response.ApiResponse;
import com.example.reviewservice.dto.response.PageResponse;
import com.example.reviewservice.dto.response.ProductReviewResponse;
import com.example.reviewservice.dto.response.ReviewSummaryResponse;
import com.example.reviewservice.service.ReviewService;
import com.example.reviewservice.entity.ReviewStatus;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.Instant;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/reviews")
public class ReviewController {
    private final ReviewService reviewService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<ProductReviewResponse> create(
            @AuthenticationPrincipal Jwt jwt,
            @RequestHeader("Authorization") String authorization,
            @Valid @RequestBody CreateReviewRequest request
    ) {
        return response(HttpStatus.CREATED, "Review created successfully",
                reviewService.createReview(jwt.getSubject(), authorization, request));
    }

    @PutMapping("/{reviewId}")
    public ApiResponse<ProductReviewResponse> update(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID reviewId,
            @Valid @RequestBody UpdateReviewRequest request
    ) {
        return response(HttpStatus.OK, "Review updated successfully",
                reviewService.updateReview(jwt.getSubject(), reviewId, request));
    }

    @DeleteMapping("/{reviewId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID reviewId) {
        reviewService.deleteReview(jwt.getSubject(), reviewId);
    }

    @GetMapping("/products/{productId}")
    public ApiResponse<PageResponse<ProductReviewResponse>> getProductReviews(
            @PathVariable String productId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return response(HttpStatus.OK, "Product reviews retrieved successfully",
                reviewService.getProductReviews(productId, page, size));
    }

    @GetMapping("/products/{productId}/summary")
    public ApiResponse<ReviewSummaryResponse> getProductSummary(@PathVariable String productId) {
        return response(HttpStatus.OK, "Review summary retrieved successfully",
                reviewService.getProductSummary(productId));
    }

    @GetMapping("/me")
    public ApiResponse<PageResponse<ProductReviewResponse>> getMyReviews(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return response(HttpStatus.OK, "My reviews retrieved successfully",
                reviewService.getMyReviews(jwt.getSubject(), page, size));
    }

    @GetMapping("/order-items/{orderItemId}")
    public ApiResponse<ProductReviewResponse> getMyReviewByOrderItem(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable String orderItemId
    ) {
        return response(HttpStatus.OK, "Review retrieved successfully",
                reviewService.getMyReviewByOrderItem(jwt.getSubject(), orderItemId));
    }

    @GetMapping("/admin")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ADMIN')")
    public ApiResponse<PageResponse<ProductReviewResponse>> getAdminReviews(
            @RequestParam(required = false) ReviewStatus status,
            @RequestParam(required = false) Integer rating,
            @RequestParam(required = false) String productId,
            @RequestParam(required = false) String userId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant createdFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant createdTo,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return response(HttpStatus.OK, "Admin reviews retrieved successfully",
                reviewService.getAdminReviews(status, rating, productId, userId,
                        createdFrom, createdTo, page, size));
    }

    @GetMapping("/{reviewId}")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ADMIN')")
    public ApiResponse<ProductReviewResponse> getReview(@PathVariable UUID reviewId) {
        return response(HttpStatus.OK, "Review retrieved successfully",
                reviewService.getReview(reviewId));
    }

    @PutMapping("/{reviewId}/moderation")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ADMIN')")
    public ApiResponse<ProductReviewResponse> moderate(
            @PathVariable UUID reviewId,
            @Valid @RequestBody ModerateReviewRequest request
    ) {
        return response(HttpStatus.OK, "Review moderation updated successfully",
                reviewService.moderateReview(reviewId, request));
    }

    @PostMapping("/{reviewId}/reply")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ADMIN')")
    public ApiResponse<ProductReviewResponse> reply(
            @PathVariable UUID reviewId,
            @Valid @RequestBody SellerReplyRequest request
    ) {
        return response(HttpStatus.OK, "Seller reply saved successfully",
                reviewService.replyReview(reviewId, request));
    }

    private <T> ApiResponse<T> response(HttpStatus status, String message, T data) {
        return ApiResponse.<T>builder()
                .status(status.value())
                .message(message)
                .data(data)
                .build();
    }
}
