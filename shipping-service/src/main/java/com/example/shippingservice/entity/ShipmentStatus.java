package com.example.shippingservice.entity;

public enum ShipmentStatus {
    CREATED,
    PACKING,
    READY_TO_SHIP,
    IN_TRANSIT,
    DELIVERED,
    DELIVERY_FAILED,
    RETURNING,
    RETURNED,
    CANCELLED
}
