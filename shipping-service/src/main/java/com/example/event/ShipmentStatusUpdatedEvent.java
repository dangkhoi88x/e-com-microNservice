package com.example.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ShipmentStatusUpdatedEvent {
    private UUID shipmentId;
    private String orderId;
    private String userId;
    private String oldStatus;
    private String newStatus;
    private String carrier;
    private String trackingNumber;
    private String description;
    private String location;
    private Instant updatedAt;
}
