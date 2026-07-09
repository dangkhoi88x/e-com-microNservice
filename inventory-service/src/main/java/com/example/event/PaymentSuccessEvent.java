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
public class PaymentSuccessEvent {
    private String paymentId;
    private String orderId;
    private String userId;
    private BigDecimal amount;
    private String method;
    private String transactionCode;
    private Instant paidAt;
}
