package com.example.paymentservice.dto.response;

import java.time.Instant;

public record StripeCheckoutResponse(
        String paymentId,
        String sessionId,
        String checkoutUrl,
        Instant expiresAt
) {
}
