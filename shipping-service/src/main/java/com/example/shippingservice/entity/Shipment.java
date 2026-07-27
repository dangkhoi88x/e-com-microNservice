package com.example.shippingservice.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.time.Instant;

@Entity
@Table(
        name = "shipments",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_shipments_order_id", columnNames = "order_id"),
                @UniqueConstraint(name = "uk_shipments_tracking_number", columnNames = "tracking_number")
        },
        indexes = {
                @Index(name = "idx_shipments_user_id", columnList = "user_id"),
                @Index(name = "idx_shipments_status", columnList = "status")
        }
)
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class Shipment extends AbstractEntity {
    @Column(name = "order_id", nullable = false, updatable = false)
    private String orderId;

    @Column(name = "user_id", nullable = false, updatable = false)
    private String userId;

    @Column(name = "shipping_address", nullable = false, columnDefinition = "TEXT")
    private String shippingAddress;

    @Column(length = 100)
    private String carrier;

    @Column(name = "tracking_number", length = 100)
    private String trackingNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ShipmentStatus status;

    @Column(name = "estimated_delivery_at")
    private Instant estimatedDeliveryAt;

    @Column(name = "shipped_at")
    private Instant shippedAt;

    @Column(name = "delivered_at")
    private Instant deliveredAt;

    @Version
    private Long version;
}
