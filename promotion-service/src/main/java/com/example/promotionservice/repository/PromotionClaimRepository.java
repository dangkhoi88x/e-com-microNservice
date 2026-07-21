package com.example.promotionservice.repository;

import com.example.promotionservice.entity.PromotionClaim;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface PromotionClaimRepository extends JpaRepository<PromotionClaim, UUID> {
    boolean existsByCampaignIdAndUserId(UUID campaignId, String userId);
    List<PromotionClaim> findAllByUserId(String userId);
}
