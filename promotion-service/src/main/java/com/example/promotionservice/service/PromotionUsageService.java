package com.example.promotionservice.service;

import com.example.promotionservice.dto.request.PromotionOrderRequest;
import com.example.promotionservice.dto.request.ReservePromotionRequest;
import com.example.promotionservice.dto.request.ValidatePromotionRequest;
import com.example.promotionservice.dto.response.PromotionCalculationResponse;
import com.example.promotionservice.dto.response.PromotionCampaignResponse;

public interface PromotionUsageService {
    PromotionCalculationResponse validate(ValidatePromotionRequest request);
    PromotionCalculationResponse reserve(ReservePromotionRequest request);
    void confirm(PromotionOrderRequest request);
    void release(PromotionOrderRequest request);
    PromotionCampaignResponse claim(String campaignId, String userId);
    java.util.List<PromotionCampaignResponse> getClaimed(String userId);
}
