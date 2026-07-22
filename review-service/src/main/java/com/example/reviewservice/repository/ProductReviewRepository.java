package com.example.reviewservice.repository;

import com.example.reviewservice.entity.ProductReview;
import com.example.reviewservice.entity.ReviewStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface ProductReviewRepository extends JpaRepository<ProductReview, UUID>, JpaSpecificationExecutor<ProductReview> {
    boolean existsByOrderItemId(String orderItemId);

    Optional<ProductReview> findByIdAndUserId(UUID id, String userId);

    Optional<ProductReview> findByOrderItemIdAndUserId(String orderItemId, String userId);

    Page<ProductReview> findByProductIdAndStatus(
            String productId,
            ReviewStatus status,
            Pageable pageable
    );

    Page<ProductReview> findByUserId(String userId, Pageable pageable);

    @Query("""
            select coalesce(avg(review.rating), 0) as averageRating,
                   count(review) as reviewCount
            from ProductReview review
            where review.productId = :productId
              and review.status = com.example.reviewservice.entity.ReviewStatus.PUBLISHED
            """)
    ReviewSummaryProjection summarizeProduct(@Param("productId") String productId);

    @Query("""
            select review.rating as rating, count(review) as reviewCount
            from ProductReview review
            where review.productId = :productId
              and review.status = com.example.reviewservice.entity.ReviewStatus.PUBLISHED
            group by review.rating
            """)
    java.util.List<RatingCountProjection> ratingDistribution(@Param("productId") String productId);
}
