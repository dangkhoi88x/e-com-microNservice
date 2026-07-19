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
public class CodPaymentCreatedEvent {
    private String paymentId;
    private String orderId;
    private String userId;
    private Instant createdAt;
}
