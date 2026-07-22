package com.example.promotionservice.repository;

import java.math.BigDecimal;

public interface FlashDealCampaignMetricProjection {
    Long getOrderCount();
    Long getSoldQuantity();
    BigDecimal getRevenue();
}
