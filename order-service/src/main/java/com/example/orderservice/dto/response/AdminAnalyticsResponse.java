package com.example.orderservice.dto.response;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public record AdminAnalyticsResponse(
        BigDecimal revenue,
        long totalOrders,
        long completedOrders,
        BigDecimal averageOrderValue,
        Map<String, Long> ordersByStatus,
        List<RevenuePoint> revenueByDate,
        List<TopProduct> topProducts
) {
    public record RevenuePoint(String date, BigDecimal revenue, long orders) { }
    public record TopProduct(String productId, String name, long quantitySold, BigDecimal revenue) { }
}
