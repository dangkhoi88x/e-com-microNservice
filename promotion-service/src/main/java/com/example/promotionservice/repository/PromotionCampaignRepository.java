package com.example.promotionservice.repository;

import com.example.promotionservice.entity.PromotionCampaign;
import com.example.promotionservice.entity.PromotionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PromotionCampaignRepository extends JpaRepository<PromotionCampaign, UUID> {
    Optional<PromotionCampaign> findByCodeIgnoreCase(String code);
    boolean existsByCodeIgnoreCase(String code);
    List<PromotionCampaign> findAllByStatusOrderByPriorityDescStartAtDesc(PromotionStatus status);

    /**
     * Atomic increment so concurrent confirmations cannot lose an update. A read-modify-write on
     * usedCount lets two payments that succeed at the same time both write the same value, which
     * silently under-counts usage and lets a campaign exceed its usageLimit.
     *
     * <p>Returns 0 when the campaign is capped, mirroring the conditional-update pattern used by
     * {@code FlashDealItemRepository.reserve}.
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update PromotionCampaign campaign set campaign.usedCount = campaign.usedCount + 1 "
            + "where campaign.id = :id and (campaign.usageLimit <= 0 or campaign.usedCount < campaign.usageLimit)")
    int incrementUsedCount(@Param("id") UUID id);
}
