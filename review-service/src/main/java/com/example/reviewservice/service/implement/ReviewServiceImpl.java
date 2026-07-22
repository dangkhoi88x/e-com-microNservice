package com.example.reviewservice.service.implement;

import com.example.reviewservice.client.OrderClient;
import com.example.reviewservice.client.ProfileClient;
import com.example.reviewservice.dto.request.CreateReviewRequest;
import com.example.reviewservice.dto.request.ModerateReviewRequest;
import com.example.reviewservice.dto.request.SellerReplyRequest;
import com.example.reviewservice.dto.request.UpdateReviewRequest;
import com.example.reviewservice.dto.response.PageResponse;
import com.example.reviewservice.dto.response.ProductReviewResponse;
import com.example.reviewservice.dto.response.ReviewEligibilityResponse;
import com.example.reviewservice.dto.response.ReviewSummaryResponse;
import com.example.reviewservice.entity.ProductReview;
import com.example.reviewservice.entity.ReviewStatus;
import com.example.reviewservice.exception.ErrorCode;
import com.example.reviewservice.exception.ReviewServiceException;
import com.example.reviewservice.mapper.ProductReviewMapper;
import com.example.reviewservice.messaging.ReviewSummaryEventPublisher;
import com.example.reviewservice.repository.ProductReviewRepository;
import com.example.reviewservice.repository.RatingCountProjection;
import com.example.reviewservice.repository.ReviewSummaryProjection;
import com.example.reviewservice.service.ReviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import event.ReviewSummaryChangedEvent;

@Service
@RequiredArgsConstructor
public class ReviewServiceImpl implements ReviewService {
    private final ProductReviewRepository reviewRepository;
    private final ProductReviewMapper reviewMapper;
    private final OrderClient orderClient;
    private final ProfileClient profileClient;
    private final ReviewSummaryEventPublisher eventPublisher;

    @Override
    @Transactional
    public ProductReviewResponse createReview(
            String userId,
            String authorization,
            CreateReviewRequest request
    ) {
        if (reviewRepository.existsByOrderItemId(request.orderItemId())) {
            throw new ReviewServiceException(ErrorCode.REVIEW_ALREADY_EXISTS);
        }

        ReviewEligibilityResponse eligibility = orderClient.checkEligibility(
                request.orderItemId(), authorization
        );
        if (eligibility == null || !eligibility.eligible()) {
            throw new ReviewServiceException(ErrorCode.REVIEW_NOT_ELIGIBLE);
        }

        String reviewerName = profileClient.getPublicReviewerName(authorization);
        ProductReview review = reviewMapper.toEntity(request, eligibility, userId, reviewerName);
        review.setStatus(ReviewStatus.PUBLISHED);
        if (review.getImages() == null) {
            review.setImages(new ArrayList<>());
        }
        ProductReview saved = reviewRepository.saveAndFlush(review);
        publishSummaryAfterCommit(saved.getProductId());
        return reviewMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public ProductReviewResponse updateReview(String userId, UUID reviewId, UpdateReviewRequest request) {
        ProductReview review = findOwnedReview(userId, reviewId);
        reviewMapper.update(request, review);
        if (review.getImages() == null) {
            review.setImages(new ArrayList<>());
        }
        ProductReview saved = reviewRepository.saveAndFlush(review);
        publishSummaryAfterCommit(saved.getProductId());
        return reviewMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public void deleteReview(String userId, UUID reviewId) {
        ProductReview review = findOwnedReview(userId, reviewId);
        String productId = review.getProductId();
        reviewRepository.delete(review);
        reviewRepository.flush();
        publishSummaryAfterCommit(productId);
    }

    @Override
    @Transactional
    public ProductReviewResponse moderateReview(UUID reviewId, ModerateReviewRequest request) {
        ProductReview review = findReview(reviewId);
        ReviewStatus previousStatus = review.getStatus();
        review.setStatus(request.status());
        ProductReview saved = reviewRepository.saveAndFlush(review);
        if (previousStatus != saved.getStatus()) {
            publishSummaryAfterCommit(saved.getProductId());
        }
        return reviewMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public ProductReviewResponse replyReview(UUID reviewId, SellerReplyRequest request) {
        ProductReview review = findReview(reviewId);
        review.setSellerReply(request.reply().trim());
        return reviewMapper.toResponse(reviewRepository.save(review));
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<ProductReviewResponse> getProductReviews(String productId, int page, int size) {
        Page<ProductReview> result = reviewRepository.findByProductIdAndStatus(
                productId,
                ReviewStatus.PUBLISHED,
                pageable(page, size)
        );
        return pageResponse(result);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<ProductReviewResponse> getMyReviews(String userId, int page, int size) {
        return pageResponse(reviewRepository.findByUserId(userId, pageable(page, size)));
    }

    @Override
    @Transactional(readOnly = true)
    public ReviewSummaryResponse getProductSummary(String productId) {
        ReviewSummaryProjection summary = reviewRepository.summarizeProduct(productId);
        double average = summary == null || summary.getAverageRating() == null
                ? 0.0
                : summary.getAverageRating();
        long count = summary == null || summary.getReviewCount() == null
                ? 0L
                : summary.getReviewCount();
        return new ReviewSummaryResponse(productId, average, count, ratingDistribution(productId));
    }

    @Override
    @Transactional(readOnly = true)
    public ProductReviewResponse getReview(UUID reviewId) {
        return reviewMapper.toResponse(findReview(reviewId));
    }

    @Override
    @Transactional(readOnly = true)
    public ProductReviewResponse getMyReviewByOrderItem(String userId, String orderItemId) {
        ProductReview review = reviewRepository.findByOrderItemIdAndUserId(orderItemId, userId)
                .orElseThrow(() -> new ReviewServiceException(ErrorCode.REVIEW_NOT_FOUND));
        return reviewMapper.toResponse(review);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<ProductReviewResponse> getAdminReviews(
            ReviewStatus status,
            Integer rating,
            String productId,
            String userId,
            Instant createdFrom,
            Instant createdTo,
            int page,
            int size
    ) {
        Specification<ProductReview> specification = (root, query, builder) -> builder.conjunction();
        if (status != null) {
            specification = specification.and((root, query, builder) -> builder.equal(root.get("status"), status));
        }
        if (rating != null) {
            specification = specification.and((root, query, builder) -> builder.equal(root.get("rating"), rating));
        }
        if (productId != null && !productId.isBlank()) {
            specification = specification.and((root, query, builder) -> builder.equal(root.get("productId"), productId));
        }
        if (userId != null && !userId.isBlank()) {
            specification = specification.and((root, query, builder) -> builder.equal(root.get("userId"), userId));
        }
        if (createdFrom != null) {
            specification = specification.and((root, query, builder) -> builder.greaterThanOrEqualTo(root.get("createdAt"), createdFrom));
        }
        if (createdTo != null) {
            specification = specification.and((root, query, builder) -> builder.lessThanOrEqualTo(root.get("createdAt"), createdTo));
        }
        return pageResponse(reviewRepository.findAll(specification, pageable(page, size)));
    }

    private ProductReview findOwnedReview(String userId, UUID reviewId) {
        return reviewRepository.findByIdAndUserId(reviewId, userId)
                .orElseThrow(() -> new ReviewServiceException(ErrorCode.REVIEW_ACCESS_DENIED));
    }

    private ProductReview findReview(UUID reviewId) {
        return reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ReviewServiceException(ErrorCode.REVIEW_NOT_FOUND));
    }

    private Pageable pageable(int page, int size) {
        int normalizedPage = Math.max(page, 1) - 1;
        int normalizedSize = Math.min(Math.max(size, 1), 100);
        return PageRequest.of(normalizedPage, normalizedSize, Sort.by(Sort.Direction.DESC, "createdAt"));
    }

    private PageResponse<ProductReviewResponse> pageResponse(Page<ProductReview> page) {
        return PageResponse.<ProductReviewResponse>builder()
                .currentPage(page.getNumber() + 1)
                .pageSize(page.getSize())
                .totalPages(page.getTotalPages())
                .totalElements(page.getTotalElements())
                .content(page.getContent().stream().map(reviewMapper::toResponse).toList())
                .build();
    }

    private Map<Integer, Long> ratingDistribution(String productId) {
        Map<Integer, Long> distribution = new LinkedHashMap<>();
        for (int rating = 5; rating >= 1; rating--) {
            distribution.put(rating, 0L);
        }
        for (RatingCountProjection item : reviewRepository.ratingDistribution(productId)) {
            distribution.put(item.getRating(), item.getReviewCount());
        }
        return distribution;
    }

    private void publishSummaryAfterCommit(String productId) {
        ReviewSummaryProjection summary = reviewRepository.summarizeProduct(productId);
        double average = summary == null || summary.getAverageRating() == null
                ? 0.0
                : summary.getAverageRating();
        long count = summary == null || summary.getReviewCount() == null
                ? 0L
                : summary.getReviewCount();
        eventPublisher.publishAfterCommit(new ReviewSummaryChangedEvent(productId, average, count));
    }
}
