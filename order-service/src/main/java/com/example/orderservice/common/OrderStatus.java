package com.example.orderservice.common;

public enum OrderStatus {
    PENDING,
    PENDING_PAYMENT,
    INVENTORY_FAILED,
    CONFIRMED,
    SHIPPING,
    COMPLETED,
    CANCELLED
}
