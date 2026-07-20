package com.example.promotionservice.repository;

import com.example.promotionservice.entity.PromotionUsage;
import com.example.promotionservice.entity.PromotionUsageStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface PromotionUsageRepository extends JpaRepository<PromotionUsage, UUID> {
    Optional<PromotionUsage> findByOrderId(String orderId);
    boolean existsByCampaignIdAndUserIdAndStatus(UUID campaignId, String userId, PromotionUsageStatus status);
}
