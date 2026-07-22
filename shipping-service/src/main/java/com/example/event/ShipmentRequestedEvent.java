package com.example.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ShipmentRequestedEvent {
    private String orderId;
    private String orderCode;
    private String userId;
    private String shippingAddress;
    private Instant requestedAt;
}
