package com.example.paymentservice.dto.response;

import java.math.BigDecimal;
import java.time.Instant;

public record PaymentResponse(String id,
                              String orderId,
                              String userId,
                              BigDecimal amount,
                              String method,
                              String status,
                              String transactionCode,
                              String failureReason,
                              String stripeCheckoutSessionId,
                              Instant paidAt,
                              Instant createdAt) {
}
