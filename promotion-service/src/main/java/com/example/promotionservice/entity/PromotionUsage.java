package com.example.promotionservice.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "promotion_usages", uniqueConstraints = @UniqueConstraint(name = "uk_promotion_usage_order", columnNames = "order_id"))
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class PromotionUsage extends AbstractEntity {
    @Column(nullable = false)
    private String userId;

    @Column(nullable = false)
    private String orderId;

    @ManyToOne(optional = false)
    @JoinColumn(name = "campaign_id", nullable = false)
    private PromotionCampaign campaign;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal discountAmount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PromotionUsageStatus status;

    private Instant expiresAt;
}
