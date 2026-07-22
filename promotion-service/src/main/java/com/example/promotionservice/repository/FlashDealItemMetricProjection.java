package com.example.promotionservice.repository;

import java.math.BigDecimal;
import java.util.UUID;

public interface FlashDealItemMetricProjection {
    UUID getItemId();
    Long getOrderCount();
    Long getSoldQuantity();
    BigDecimal getRevenue();
}
