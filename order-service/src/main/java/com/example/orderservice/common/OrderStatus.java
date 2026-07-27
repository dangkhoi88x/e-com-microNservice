package com.example.orderservice.common;

public enum OrderStatus {
    PENDING,
    PENDING_PAYMENT,
    INVENTORY_FAILED,
    PROMOTION_FAILED,
    CONFIRMED,
    SHIPPING,
    DELIVERY_FAILED,
    RETURNING,
    RETURNED,
    COMPLETED,
    CANCELLED
}
