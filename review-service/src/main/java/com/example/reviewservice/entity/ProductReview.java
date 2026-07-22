package com.example.reviewservice.entity;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(
        name = "product_reviews",
        uniqueConstraints = @UniqueConstraint(name = "uk_review_order_item", columnNames = "order_item_id"),
        indexes = {
                @Index(name = "idx_review_product", columnList = "product_id"),
                @Index(name = "idx_review_user", columnList = "user_id"),
                @Index(name = "idx_review_status", columnList = "status")
        }
)
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class ProductReview extends AbstractEntity {
    @Column(name = "product_id", nullable = false)
    private String productId;

    @Column(name = "variant_id")
    private String variantId;

    @Column(name = "user_id", nullable = false)
    private String userId;

    @Column(name = "reviewer_name", length = 120)
    private String reviewerName;

    @Column(name = "verified_purchase", nullable = false, columnDefinition = "boolean default true")
    @Builder.Default
    private boolean verifiedPurchase = true;

    @Column(name = "order_id", nullable = false)
    private String orderId;

    @Column(name = "order_item_id", nullable = false)
    private String orderItemId;

    @Column(nullable = false)
    private Integer rating;

    @Column(length = 3000)
    private String content;

    @ElementCollection
    @CollectionTable(name = "product_review_images", joinColumns = @JoinColumn(name = "review_id"))
    @Column(name = "image_url", length = 1000)
    @OrderColumn(name = "display_order")
    @Builder.Default
    private List<String> images = new ArrayList<>();

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private ReviewStatus status = ReviewStatus.PUBLISHED;

    @Column(length = 2000)
    private String sellerReply;
}
