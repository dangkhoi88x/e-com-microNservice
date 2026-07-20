package com.example.promotionservice.service;

import com.example.promotionservice.dto.request.CreatePromotionCampaignRequest;
import com.example.promotionservice.dto.request.UpdatePromotionCampaignRequest;
import com.example.promotionservice.dto.response.PromotionCampaignResponse;

import java.util.List;

public interface PromotionCampaignService {
    PromotionCampaignResponse create(CreatePromotionCampaignRequest request);
    List<PromotionCampaignResponse> getAll(String status);
    PromotionCampaignResponse getById(String id);
    PromotionCampaignResponse update(String id, UpdatePromotionCampaignRequest request);
    void delete(String id);
}
