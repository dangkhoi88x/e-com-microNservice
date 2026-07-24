package com.example.orderservice.dto.response;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public record SellerAnalyticsResponse(BigDecimal revenue, long completedOrders, long totalOrders, BigDecimal averageOrderValue, Map<String, Long> ordersByStatus, List<TopProduct> topProducts) {
    public record TopProduct(String productId, String name, long quantitySold, BigDecimal revenue) { }
}
