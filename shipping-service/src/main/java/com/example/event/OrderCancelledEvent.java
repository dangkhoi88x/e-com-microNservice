package com.example.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderCancelledEvent {
    private String orderId;
    private String orderCode;
    private String userId;
    private BigDecimal totalAmount;
    private String status;
    private Instant cancelledAt;
}
