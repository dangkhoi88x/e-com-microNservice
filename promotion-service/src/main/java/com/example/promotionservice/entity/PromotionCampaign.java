package com.example.promotionservice.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
@Table(name = "promotion_campaigns", uniqueConstraints = @UniqueConstraint(name = "uk_promotion_campaign_code", columnNames = "code"))
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class PromotionCampaign extends AbstractEntity {
    @Column(nullable = false)
    private String name;

    @Column(nullable = false, length = 60)
    private String code;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private PromotionType type;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal discountValue;

    @Column(precision = 19, scale = 2)
    private BigDecimal maxDiscountAmount;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal minOrderAmount;

    @Column(nullable = false)
    private Instant startAt;

    @Column(nullable = false)
    private Instant endAt;

    @Column(nullable = false)
    private Integer usageLimit;

    @Column(nullable = false)
    private Integer usedCount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PromotionStatus status;

    @Column(columnDefinition = "TEXT")
    private String applicableCategoryIdsJson;

    @Column(columnDefinition = "TEXT")
    private String applicableProductIdsJson;

    @Column(nullable = false)
    private Integer priority;

    @Column(nullable = false)
    private boolean stackable;
}
