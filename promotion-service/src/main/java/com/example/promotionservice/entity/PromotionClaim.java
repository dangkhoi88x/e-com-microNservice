package com.example.promotionservice.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "promotion_claims", uniqueConstraints = @UniqueConstraint(name = "uk_promotion_claim_user_campaign", columnNames = {"user_id", "campaign_id"}))
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class PromotionClaim extends AbstractEntity {
    @Column(nullable = false)
    private String userId;

    @ManyToOne(optional = false)
    @JoinColumn(name = "campaign_id", nullable = false)
    private PromotionCampaign campaign;
}
