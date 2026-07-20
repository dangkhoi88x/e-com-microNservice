package com.example.promotionservice.repository;

import com.example.promotionservice.entity.PromotionCampaign;
import com.example.promotionservice.entity.PromotionStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PromotionCampaignRepository extends JpaRepository<PromotionCampaign, UUID> {
    Optional<PromotionCampaign> findByCodeIgnoreCase(String code);
    boolean existsByCodeIgnoreCase(String code);
    List<PromotionCampaign> findAllByStatusOrderByPriorityDescStartAtDesc(PromotionStatus status);
}
